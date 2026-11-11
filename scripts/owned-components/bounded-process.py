#!/usr/bin/env python3
"""Bounded retained-stage producer execution. No app edits, automatic pins or target execution."""
import argparse
import hashlib
import json
import os
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

SCRIPTS = ['build-python-dependencies-arm64.sh', 'refresh-compression-arm64.sh',
           'refresh-remaining-arm64.sh', 'build-pycryptodomex-arm64.sh', 'build-cpython-arm64.sh']
MAX_WORK_BYTES = 20 * 1024**3
MIN_FREE_BYTES = 6 * 1024**3


def require(ok, why):
    if not ok:
        raise ValueError(why)


def sha(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def inventory(root):
    rows = []
    for f in sorted(root.rglob('*')):
        rel = str(f.relative_to(root))
        if f.is_symlink():
            rows.append({'path': rel, 'kind': 'symlink', 'target': os.readlink(f)})
        elif f.is_file():
            rows.append({'path': rel, 'kind': 'file', 'bytes': f.stat().st_size, 'sha256': sha(f)})
    return rows


def work_bytes(root):
    return sum(f.lstat().st_size for f in root.rglob('*') if not f.is_symlink() and f.is_file())


def bounded(command, env, log, timeout, work, monitor_seconds=5):
    """Terminate the child process group on error/timeout. Detached sessions are not covered."""
    started = time.monotonic()
    stop_reason = None
    with log.open('wb') as output:
        proc = subprocess.Popen(command, env=env, stdout=output, stderr=subprocess.STDOUT,
                                start_new_session=True, cwd=work)
        try:
            while proc.poll() is None:
                if time.monotonic() - started >= timeout:
                    stop_reason = 'wall timeout'
                elif shutil.disk_usage(work).free < MIN_FREE_BYTES:
                    stop_reason = 'free disk below 6 GiB'
                elif work_bytes(work) > MAX_WORK_BYTES:
                    stop_reason = 'work tree above 20 GiB'
                if stop_reason:
                    break
                time.sleep(monitor_seconds)
        except BaseException as error:
            stop_reason = 'interrupted: ' + type(error).__name__ + ': ' + str(error)
        finally:
            if proc.poll() is None:
                os.killpg(proc.pid, signal.SIGTERM)
                try:
                    proc.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    pass
                # A descendant can ignore TERM after the leader exits. KILL the
                # original group even if wait already reaped the leader.
                try:
                    os.killpg(proc.pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass
                proc.wait()
            # A normally exited leader is not proof its descendants exited.
            try:
                os.killpg(proc.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
    return {'command': command, 'seconds': round(time.monotonic() - started, 3),
            'exit_code': proc.returncode, 'stop_reason': stop_reason,
            'log': str(log), 'log_sha256': sha(log)}


def validate_plan(plan, prepared, work):
    phases = plan['phases']
    require(len(phases) == 5, 'phase count differs')
    require(plan['executed_phases'] == [], 'preflight must not claim compilation')
    for phase, script in zip(phases, SCRIPTS):
        require(phase['command'] == ['bash', str(prepared / 'recipe' / script)], 'unexpected phase command')
        require(phase['status'] == 'NOT_RUN' and 1 <= phase['timeout_seconds'] <= 1800, 'unexpected phase status/bound')
        require(Path(phase['working_output']).parent == work, 'phase output outside fresh work root')
        require(set(phase['environment']) <= {'ANDROID_NDK_ROOT', 'INPUTS', 'TERMUX_SOURCE',
                'OWNED_COMPONENT_STAGE', 'WORK', 'BASE_PREFIX', 'SOURCE_ARCHIVE', 'PYTHON_ARCHIVE',
                'SOURCE_BUILT_DEP_PREFIX', 'BUILD_PYTHON'}, 'unexpected environment field')
        require(phase['environment']['WORK'] == phase['working_output'], 'work environment differs')
    return phases


def make_environment(work):
    env = {'PATH': '/usr/bin:/bin', 'HOME': str(work / 'home'), 'LANG': 'C.UTF-8',
           'LC_ALL': 'C.UTF-8', 'PYTHONNOUSERSITE': '1', 'PYTHONDONTWRITEBYTECODE': '1',
           'SOURCE_DATE_EPOCH': '1641013200', 'TMPDIR': str(work / 'tmp')}
    Path(env['HOME']).mkdir(mode=0o700)
    Path(env['TMPDIR']).mkdir(mode=0o700)
    return env


def main():
    p = argparse.ArgumentParser()
    for flag in ['prepared', 'ndk', 'ndk-archive', 'qualified-components-stage', 'build-python', 'work', 'receipt', 'producer-lock', 'stage-lock']:
        p.add_argument('--' + flag, required=True)
    p.add_argument('--execute', action='store_true')
    p.add_argument('--first-phase-only', action='store_true')
    a = p.parse_args()
    work, prepared = Path(a.work), Path(a.prepared)
    require(work.is_absolute() and work.parent.is_dir() and work.parent.resolve() == work.parent,
            'work parent must exist without symlink traversal')
    require(not work.exists() and not work.is_symlink(), 'fresh work root required')
    require(shutil.disk_usage(work.parent).free >= MIN_FREE_BYTES, 'insufficient initial free disk')
    # A plan JSON cannot supply code: regenerate it from the checked prerequisite
    # script, then enforce exact known phase commands before execution.
    preflight = Path(__file__).with_name('seali-check-producer-prerequisites.py')
    result = Path(a.receipt)
    require(not result.exists() and result.parent.is_dir(), 'fresh receipt required')
    command = [sys.executable, str(preflight)]
    for name in ['prepared', 'ndk', 'ndk-archive', 'qualified-components-stage', 'build-python', 'work', 'receipt', 'producer-lock', 'stage-lock']:
        command += ['--' + name, getattr(a, name.replace('-', '_'))]
    subprocess.run(command, check=True, timeout=180)
    plan = json.loads(result.read_text())
    phases = validate_plan(plan, prepared, work)
    if a.first_phase_only:
        phases = phases[:1]
    plan['execution_phase_limit'] = len(phases)
    plan['orchestrator_sha256'] = sha(Path(__file__))
    plan['preflight_sha256'] = sha(preflight)
    plan['recipe_snapshot'] = inventory(prepared / 'recipe')
    plan['resource_bounds'] = {'phase_wall_seconds_max': 1800, 'work_bytes_max': MAX_WORK_BYTES,
            'disk_free_min': MIN_FREE_BYTES, 'monitor_seconds': 5, 'compiler_make_jobs': 2,
            'aggregate_ram_limit_enforced': False, 'cgroup_enforced': False, 'detached_session_cleanup_guaranteed': False, 'no_timeout_extension_or_automatic_retry': True}
    plan['status'] = 'PLAN_ONLY'
    result.write_text(json.dumps(plan, indent=2) + '\n')
    if not a.execute:
        print('PLAN_ONLY, no build executed: ' + str(result))
        return
    require(plan['network_isolation_probe']['available'], 'network namespace isolation required before execute')
    work.mkdir(mode=0o700)
    (work / 'logs').mkdir(mode=0o700)
    # No credentials, inherited C/LD/Python flags, proxies or environment secrets.
    env = make_environment(work)
    plan['status'] = 'RUNNING'
    def recheck(index):
        require(sha(preflight) == plan['preflight_sha256'], 'preflight script drift')
        require(sha(Path(__file__).with_name('seali-producer-input-validation.py')) == plan['input_validation_sha256'], 'validation helper drift')
        recheck_path = work / 'logs' / ('immutable-recheck-' + str(index) + '.json')
        recheck_command = command[:]
        recheck_command[recheck_command.index('--receipt') + 1] = str(recheck_path)
        recheck_command += ['--recheck']
        with (work / 'logs' / ('immutable-recheck-' + str(index) + '.log')).open('wb') as stream:
            subprocess.run(recheck_command, check=True, timeout=180, env=env,
                           stdout=stream, stderr=subprocess.STDOUT)
        checked = json.loads(recheck_path.read_text())
        require(checked['host_tools_and_os'] == plan['host_tools_and_os'], 'host tools/OS drift')
        require(checked['network_isolation_probe']['available'], 'network isolation unavailable')
        require(inventory(prepared / 'recipe') == plan['recipe_snapshot'], 'recipe drift during execution')

    try:
        for index, phase in enumerate(phases):
            # Recipes may mutate inputs themselves, even without another writer.
            recheck(index)
            row = bounded(plan['network_isolation_probe']['command_prefix'] + phase['command'], {**env, **phase['environment']},
                          work / 'logs' / (phase['name'] + '.log'), phase['timeout_seconds'], work)
            row['name'] = phase['name']
            row['environment'] = {**env, **phase['environment']}
            plan['executed_phases'].append(row)
            result.write_text(json.dumps(plan, indent=2) + '\n')
            require(row['exit_code'] == 0 and row['stop_reason'] is None, 'phase failed: ' + phase['name'])
        recheck('final')
        plan['status'] = ('FIRST_PHASE_COMPLETE_REVIEW_PENDING' if a.first_phase_only else
                          'COMPILED_ONLY_PACKAGING_AND_QUALIFICATION_PENDING')
    except BaseException as error:
        plan['status'] = 'FAILED_STOPPED_NO_RETRY'
        plan['failure'] = type(error).__name__ + ': ' + str(error)
        raise
    finally:
        # Include objects, .d files, configs, archives and logs, not merely final .so.
        rows = inventory(work)
        custody = result.with_name(result.stem + '-custody.json')
        custody.write_text(json.dumps({'root': str(work), 'files': rows}, indent=2) + '\n')
        plan['custody'] = {'path': str(custody), 'sha256': sha(custody), 'entries': len(rows)}
        plan['runtime_pass'] = False
        plan['release_qualified'] = False
        result.write_text(json.dumps(plan, indent=2) + '\n')


def interrupt_signal(signum, frame):
    raise InterruptedError('received signal ' + str(signum))


if __name__ == '__main__':
    signal.signal(signal.SIGTERM, interrupt_signal)
    signal.signal(signal.SIGHUP, interrupt_signal)
    main()
