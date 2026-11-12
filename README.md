# FD Player v1.0.0

**By [MiladMedia](https://ble.ir/miladmedia_ir)**

[فارسی](#فارسی) · [English](#english)

---

## English

FD Player is an Android music player with an audio-reactive background, a download manager with duplicate prevention, and Bluetooth auto-play.

### Features
- Media3 ExoPlayer in a foreground media service with a media notification
- Swipe left = next, swipe right = previous; shuffle, repeat, seekbar, resume last track
- Audio-reactive background (FFT on the PCM stream, no `Visualizer`)
- Library: Downloads, Device audio, Playlists; Persian/Arabic-aware filtering
- Downloader: sources, source tester, keyword search quota, SHA-256 duplicate detection, resumable downloads
- Bluetooth: auto-play on connect, pause on disconnect, per-device schedules and playlist rules
- Driving mode, per-app language (fa / ar / en), RTL support, light/dark theme

### Build
1. Push to GitHub; the workflow `.github/workflows/build-apk.yml` builds the debug APK and uploads the artifact **FD-Player-Debug-APK**.
2. Locally: install JDK 17 and Android SDK, run `gradle wrapper --gradle-version 8.9` once, then `./gradlew assembleDebug`.
3. Vazirmatn v33.003 fonts go in `app/src/main/res/font/` (the CI downloads them if missing).

### Permissions
`INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE` (+ media playback / data sync / connected device), `POST_NOTIFICATIONS`, `READ_MEDIA_AUDIO` (Android 13+), `READ_EXTERNAL_STORAGE` (≤ 12), `BLUETOOTH_CONNECT` (Android 12+), `BLUETOOTH` (≤ 11), `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`.

### Bluetooth auto-play and battery
Android 12+ may block starting foreground services from the background. For reliable Bluetooth auto-play, set the app's battery usage to **Unrestricted** (Settings → Apps → FD Player → Battery → Unrestricted). If the monitor cannot start, open the app and toggle it again on the Bluetooth screen.

---

## فارسی

اف‌دی پلیر یک پخش‌کننده موسیقی اندروید با پس‌زمینه واکنش‌گرا به صدا، مدیریت دانلود با جلوگیری از تکراری و پخش خودکار بلوتوثی است.

### امکانات
- پخش با Media3 ExoPlayer در سرویس پیش‌زمینه با اعلان رسانه
- کشیدن به چپ = بعدی، به راست = قبلی؛ تصادفی، تکرار، نوار جستجو، ازسرگیری آخرین آهنگ
- پس‌زمینه واکنش‌گرا به صدا (FFT روی PCM، بدون `Visualizer`)
- کتابخانه: دانلودها، صوتی‌های دستگاه، لیست‌های پخش؛ فیلتر سازگار با فارسی/عربی
- دانلود: منابع، تست منبع، سهمیه جستجوی کلیدواژه، تشخیص تکراری با SHA-256، دانلود با قابلیت ادامه
- بلوتوث: پخش خودکار هنگام اتصال، مکث هنگام قطع، زمان‌بندی و لیست پخش برای هر دستگاه
- حالت رانندگی، زبان هر برنامه (فارسی / عربی / انگلیسی)، پشتیبانی RTL، تم روشن/تیره

### ساخت
۱. پروژه را روی گیت‌هاب بفرستید؛ ورک‌فلو `build-apk.yml` فایل APK دیباگ را می‌سازد و با نام **FD-Player-Debug-APK** آپلود می‌کند.
۲. به‌صورت محلی: JDK 17 و Android SDK را نصب کنید، یک‌بار `gradle wrapper --gradle-version 8.9` و سپس `./gradlew assembleDebug`.
۳. فونت‌های Vazirmatn نسخه 33.003 در `app/src/main/res/font/` قرار می‌گیرند (CI در صورت نبودن دانلود می‌کند).

### مجوزها
اینترنت، وضعیت شبکه، سرویس پیش‌زمینه (پخش رسانه / همگام‌سازی داده / دستگاه متصل)، اعلان‌ها، دسترسی به فایل‌های صوتی، بلوتوث (`BLUETOOTH_CONNECT` در اندروید ۱۲+)، شروع پس از بوت و WAKE_LOCK.

### پخش خودکار بلوتوث و باتری
اندروید ۱۲ به بالا ممکن است شروع سرویس پیش‌زمینه از پس‌زمینه را مسدود کند. برای پخش خودکار پایدار، مصرف باتری برنامه را روی **Unrestricted (بدون محدودیت)** بگذارید (تنظیمات ← برنامه‌ها ← FD Player ← باتری ← بدون محدودیت). اگر مانیتور اجرا نشد، برنامه را باز کنید و در صفحه بلوتوث دوباره فعالش کنید.

---
Developer / سازنده: [MiladMedia](https://ble.ir/miladmedia_ir) · MIT License
