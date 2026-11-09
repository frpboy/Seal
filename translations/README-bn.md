# Seali - current project notice

This inherited translation is not yet fully updated or validated for Seali. Use the [current README](../README.md) and [release page](https://github.com/frpboy/Seali/releases) for current status. No stable or F-Droid Seali release is claimed here. Screenshots and feature descriptions below may describe the original app and are not proof of the current Seali build.

Maintainer: Rahul Muraleedharan. Contact: [frpboy12@gmail.com](mailto:frpboy12@gmail.com). Support: [FrpBoy Projects](https://t.me/FRPboy_Projects). Optional sponsorship: [Buy Me a Coffee](https://buymeacoffee.com/frpboy) or [Patreon](https://www.patreon.com/c/FrpBoy).

Thank you to **JunkFood02** and contributors for the original [Seal project](https://github.com/JunkFood02/Seal). Original authorship and GPL-3.0 notices are preserved. See [CREDITS.md](../CREDITS.md). The links below have been updated for Seali; the inherited translation still needs language review.

---

<div align="center">

<img width="" src="../fastlane/metadata/android/en-US/images/icon.png"  width=160 height=160  align="center">

# Seali

### Video/Audio Downloader for Android


English
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/frpboy/Seali/blob/main/translations/README-zh_Hans.md">简体中文</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/frpboy/Seali/blob/main/translations/README-zh_Hant.md">繁體中文</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/frpboy/Seali/blob/main/translations/README-ar.md">العربية</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/frpboy/Seali/blob/main/translations/README-pt.md">Portuguese</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/frpboy/Seali/blob/main/translations/README-ua.md">Українська</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/frpboy/Seali/blob/main/translations/README-th.md">ภาษาไทย</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/frpboy/Seali/blob/main/translations/README-fa.md">فارسی</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/frpboy/Seali/blob/main/translations/README-it.md">Italiano</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/frpboy/Seali/blob/main/translations/README-az.md">Azərbaycanca</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/frpboy/Seali/blob/main/translations/README-ru.md">Русский</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/frpboy/Seali/blob/main/translations/README-sr.md">Српски</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/frpboy/Seali/blob/main/translations/README-ja.md">日本語</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/frpboy/Seali/blob/main/translations/README-id.md">Indonesia</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/frpboy/Seali/blob/main/translations/README-hi.md">हिंदी</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/frpboy/Seali/blob/main/translations/README-bn.md">বাংলা</a>

[![Keep a Changelog](https://img.shields.io/badge/Changelog-lightgray?style=flat&color=gray&logo=keep-a-changelog)](https://github.com/frpboy/Seali/blob/main/CHANGELOG.md)
[![GitHub all releases](https://img.shields.io/github/downloads/frpboy/Seali/total?label=Downloads&logo=github)](https://github.com/frpboy/Seali/releases/)
[![GitHub Repo stars](https://img.shields.io/github/stars/frpboy/Seali?style=flat&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4KPHN2ZyBoZWlnaHQ9IjI0IiB2aWV3Qm94PSIwIC05NjAgOTYwIDk2MCIgd2lkdGg9IjI0IiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPgogIDxwYXRoIGQ9Im0zNTQtMjQ3IDEyNi03NiAxMjYgNzctMzMtMTQ0IDExMS05Ni0xNDYtMTMtNTgtMTM2LTU4IDEzNS0xNDYgMTMgMTExIDk3LTMzIDE0M1pNMjMzLTgwbDY1LTI4MUw4MC01NTBsMjg4LTI1IDExMi0yNjUgMTEyIDI2NSAyODggMjUtMjE4IDE4OSA2NSAyODEtMjQ3LTE0OUwyMzMtODBabTI0Ny0zNTBaIiBzdHlsZT0iZmlsbDogcmdiKDI0NSwgMjI3LCA2Nik7Ii8%2BCjwvc3ZnPg%3D%3D&color=%23f8e444)](https://github.com/frpboy/Seali/stargazers)
[![Supported-Sites](https://img.shields.io/badge/Sites-9cf?style=flat&logo=data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4KPHN2ZyBoZWlnaHQ9IjI0cHgiIHZpZXdCb3g9IjAgMCAyNCAyNCIgd2lkdGg9IjI0cHgiIGZpbGw9IiNGRkZGRkYiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CiAgPHBhdGggZD0iTTAgMGgyNHYyNEgwVjB6IiBmaWxsPSJub25lIi8+CiAgPHBhdGggZD0iTTExLjk5IDJDNi40NyAyIDIgNi40OCAyIDEyczQuNDcgMTAgOS45OSAxMEMxNy41MiAyMiAyMiAxNy41MiAyMiAxMlMxNy41MiAyIDExLjk5IDJ6bTYuOTMgNmgtMi45NWMtLjMyLTEuMjUtLjc4LTIuNDUtMS4zOC0zLjU2IDEuODQuNjMgMy4zNyAxLjkxIDQuMzMgMy41NnpNMTIgNC4wNGMuODMgMS4yIDEuNDggMi41MyAxLjkxIDMuOTZoLTMuODJjLjQzLTEuNDMgMS4wOC0yLjc2IDEuOTEtMy45NnpNNC4yNiAxNEM0LjEgMTMuMzYgNCAxMi42OSA0IDEycy4xLTEuMzYuMjYtMmgzLjM4Yy0uMDguNjYtLjE0IDEuMzItLjE0IDJzLjA2IDEuMzQuMTQgMkg0LjI2em0uODIgMmgyLjk1Yy4zMiAxLjI1Ljc4IDIuNDUgMS4zOCAzLjU2LTEuODQtLjYzLTMuMzctMS45LTQuMzMtMy41NnptMi45NS04SDUuMDhjLjk2LTEuNjYgMi40OS0yLjkzIDQuMzMtMy41NkM4LjgxIDUuNTUgOC4zNSA2Ljc1IDguMDMgOHpNMTIgMTkuOTZjLS44My0xLjItMS40OC0yLjUzLTEuOTEtMy45NmgzLjgyYy0uNDMgMS40My0xLjA4IDIuNzYtMS45MSAzLjk2ek0xNC4zNCAxNEg5LjY2Yy0uMDktLjY2LS4xNi0xLjMyLS4xNi0ycy4wNy0xLjM1LjE2LTJoNC42OGMuMDkuNjUuMTYgMS4zMi4xNiAycy0uMDcgMS4zNC0uMTYgMnptLjI1IDUuNTZjLjYtMS4xMSAxLjA2LTIuMzEgMS4zOC0zLjU2aDIuOTVjLS45NiAxLjY1LTIuNDkgMi45My00LjMzIDMuNTZ6TTE2LjM2IDE0Yy4wOC0uNjYuMTQtMS4zMi4xNC0ycy0uMDYtMS4zNC0uMTQtMmgzLjM4Yy4xNi42NC4yNiAxLjMxLjI2IDJzLS4xIDEuMzYtLjI2IDJoLTMuMzh6IiBzdHlsZT0iZmlsbDogcmdiKDE2MiwgMTk4LCAyMzQpOyIvPgo8L3N2Zz4=&label=Supported)](https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md)
[![Telegram Channel](https://img.shields.io/badge/Telegram-Seali-blue?style=flat&logo=telegram)](https://t.me/FRPboy_Projects)
</div>


## 📱  স্ক্রিনশট

<div align="center">
<div>
<img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg" width="30%" />
<img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg" width="30%" />
<img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg" width="30%" />
<img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/4.jpg" width="30%" />
<img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/5.jpg" width="30%" />
<img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/6.jpg" width="30%" />
<img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/7.jpg" width="30%" />
<img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/8.jpg" width="30%" />
<img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/9.jpg" width="30%" />
</div>
</div>

<br>

## 📖 ফিচারস

- [yt-dlp](https://github.com/yt-dlp/yt-dlp) (পূর্বের youtube-dl) সাপোর্টেড ভিডিও প্ল্যাটফর্ম থেকে ভিডিও এবং অডিও ফাইল ডাউনলোড করুন।

- [mutagen](https://github.com/quodlibet/mutagen) সাপোর্টেড এক্সট্র্যাক্টটেড অডিও ফাইলগুলিতে মেটাডেটা এবং ভিডিও থাম্বনেল এমবেডেড।

- এক ক্লিকে প্লে লিস্টের সব ভিডিও ডাউনলোড করুন।

- সব ডাউনলোডের জন্য ডাউনলোডার হিসাবে এমবেডেড [aria2c](https://github.com/aria2/aria2) .

- ডাউনলোড করা ভিডিওগুলিতে সাবটাইটেল এম্বেড।

- টেমপ্লেট সহ কাস্টম yt-dlp কমান্ড রান করা।

- ইন-অ্যাপ ডাউনলোড এবং কাস্টম কমান্ড টেমপ্লেট এর ব্যাবহার।

- ব্যবহার করা সহজ এবং ইউজার ফ্রেন্ডলি।

- [Material Design 3](https://m3.material.io/) স্টাইলের UI, ডাইনামিক থিম সহ।

- MAD: UI এবং logic pure Kotlin এ কোড করা. একক কার্যকলাপ, কোন টুকরা নেই, শুধুমাত্র কমপোজ যোগ্য গন্তব্য।



## ⬇️ ডাউনলোড

Current Seali guidance (English, pending translation): download an alpha APK from the [Seali release page](https://github.com/frpboy/Seali/releases). Choose universal if unsure which ABI your device uses. No stable or F-Droid Seali release is claimed. Read the [current status](../SEALI_STATUS.md) before installing.

## 💬 যোগাযোগ

Current Seali contact (English, pending translation): [frpboy12@gmail.com](mailto:frpboy12@gmail.com) or [FrpBoy Projects on Telegram](https://t.me/FRPboy_Projects). Do not send passwords, cookies or other secrets.

## 💖 স্পনসর

Optional support for Rahul, Seali's current maintainer: [Buy Me a Coffee](https://buymeacoffee.com/frpboy) or [Patreon](https://www.patreon.com/c/FrpBoy).

Historical Seal sponsor acknowledgements, not a list of Seali sponsors:

<p><!-- sponsors --><a href="https://github.com/4kaimar"><img src="https://github.com/4kaimar.png" width="60px" alt="" /></a><a href="https://github.com/gordongw"><img src="https://github.com/gordongw.png" width="60px" alt="Gordon" /></a><a href="https://github.com/zuble"><img src="https://github.com/zuble.png" width="60px" alt="zuble" /></a><a href="https://github.com/Dannyordaniel"><img src="https://github.com/Dannyordaniel.png" width="60px" alt="Daniel " /></a><a href="https://github.com/NRN83"><img src="https://github.com/NRN83.png" width="60px" alt="" /></a><!-- sponsors --></p>

## 🤝 অবদান

Current Seali guidance (English, pending translation): use the [translation contribution guide](../CONTRIBUTING.md#translations) to submit Android string or README changes through a Seali pull request. Seali does not have its own Hosted Weblate project. Do not submit Seali changes to the original project's translation service.

## ⭐️ স্টার চার্ট

[![Star History Chart](https://api.star-history.com/svg?repos=frpboy/Seali&type=Timeline)](https://star-history.com/#frpboy/Seali&Timeline)


## 🧱 ক্রেডিট

[youtubedl-android](https://github.com/yausername/youtubedl-android) -এর উপর ভিত্তি করে সিল  [yt-dlp](https://github.com/yt-dlp/yt-dlp) -এর একটি সাধারণ GUI,

কিছু UI ডিজাইন এবং কোড [Read You](https://github.com/Ashinch/ReadYou) এবং [Music You](https://github.com/Kyant0/MusicYou) থেকে ধার করা হয়েছে।

[dvd](https://github.com/yausername/dvd)

[Material color utilities](https://github.com/material-foundation/material-color-utilities)

[Monet](https://github.com/Kyant0/Monet)

## 📃  লাইসেন্স

[![GitHub](https://img.shields.io/github/license/frpboy/Seali?style=for-the-badge)](https://github.com/frpboy/Seali/blob/main/LICENSE)

>[!সতর্কতা]
>
>GPLv3 লাইসেন্সের অধীনে লাইসেন্সকৃত সোর্স কোড ব্যতীত, অন্যান্য সমস্ত পক্ষকে ডাউনলোডার অ্যাপ
>হিসাবে সিলের নাম ব্যবহার করা নিষিদ্ধ এবং সিলের ডেরিভেটিভের ক্ষেত্রেও এটি সত্য। ডেরিভেটিভস
>অন্তর্ভুক্ত কিন্তু ফোর্ক এবং অফিসিয়াল বিল্ডের মধ্যে সীমাবদ্ধ নয়।

<div align="right">
<table><td>
<a href="#start-of-content">👆 উপরে স্ক্রোল করুন</a>
</td></table>
</div>
