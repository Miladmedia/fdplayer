# FD Player (اف دی پلیر) — نسخه 1.0.0

پخش‌کنندهٔ موسیقی مدرن و آفلاین‌محور اندروید با طراحی شیشه‌ای (Glassmorphism)، پس‌زمینهٔ پویای لاوا‌لمپ واکنش‌گرا به رنگ آلبوم و ریتم صدا، حالت رانندگی ایمن، دانلودر هوشمند لینک‌های صوتی از صفحات وب و پخش خودکار هوشمند هنگام اتصال بلوتوث.

توسعه‌دهنده: **MiladMedia** ([کانال بله](https://ble.ir/miladmedia_ir))

---

## امکانات کلیدی (فارسی)

* **پخش‌کنندهٔ روان با Media3 (ExoPlayer):** نمایش کاور آلبوم با وضوح بالا، کنترل‌های کامل پخش، تکرار و پخش تصادفی، بدون پردازش‌های سنگین FFT.
* **پس‌زمینهٔ لاوا‌لمپ (Lava Lamp):** استخراج خودکار پالت رنگی کاور آلبوم و انیمیشن نرم سیال؛ همراه با قابلیت اختیاری واکنش به ریتم محیط از طریق میکروفون (با کسب اجازه و رضایت کاربر).
* **حالت رانندگی (Driving Mode):** بزرگ‌نمایی دکمه‌های کنترل تا حدود نصف صفحه (آیکون‌های ۵۲dp)، کوچک‌سازی کاور آلبوم (۱۲۰dp) و روشن نگه داشتن صفحه برای ایمنی بیشتر حین رانندگی.
* **پخش خودکار هوشمند با بلوتوث:** تعریف قوانین بر اساس دستگاه بلوتوث (یک دستگاه خاص یا همهٔ دستگاه‌ها)، روزهای هفته، بازهٔ زمانی دلخواه (حتی بازه‌های عبوری از نیمه‌شب)، انتخاب پلی‌لیست و تاخیر شروع پخش. اگر قانونی مطابقت نداشت، پلی‌لیست جاری پخش می‌شود.
* **دانلودر هوشمند صفحات وب:** وارد کردن لینک صفحهٔ وب، استخراج خودکار تمام فایل‌های صوتی قابل دانلود (`.mp3`, `.m4a`, `.flac`, `.ogg`, `.wav`, `.aac`) و ذخیره در پوشهٔ `Downloads/FD Player` حافظهٔ دستگاه.
* **تم چهارحالته و سفارشی‌سازی:** پشتیبانی از حالت تاریک، روشن، سیستم و «دلخواه» با ۸ پالت رنگی اختصاصی و تضمین خوانایی متن (`ensureContrast`).
* **چندزبانه (RTL / LTR):** پشتیبانی کامل از فارسی (پیش‌فرض)، عربی و انگلیسی با فونت وزیرمتن (Vazirmatn) و تغییر زبان در سطح برنامه.

> **نکتهٔ مهم برای پخش خودکار بلوتوث:**
> برای اینکه سیستم‌عامل اندروید سرویس پس‌زمینهٔ بلوتوث را نبندد، در تنظیمات گوشی وارد بخش **باتری برنامه (App Battery Usage)** شوید و وضعیت **FD Player** را روی **«بدون محدودیت» (Unrestricted)** قرار دهید.

---

## Key Features (English)

* **Smooth Media3 (ExoPlayer) Playback:** High-res album artwork, intuitive controls, collapsible shuffle/repeat bar, and lightweight 500ms position updates without heavy FFT overhead.
* **Dynamic Lava Lamp Background:** Automatically extracts dominant colors from album art to animate a smooth glassmorphic lava lamp backdrop, with optional microphone rhythm reactivity (user consent + `RECORD_AUDIO` required).
* **Safe Driving Mode:** Expands playback controls to ~50% of the screen with 52dp icons, shrinks album art to 120dp, and keeps the screen awake (`FLAG_KEEP_SCREEN_ON`).
* **Smart Bluetooth Autoplay:** Create rules targeting specific or all Bluetooth devices, weekdays, custom time ranges (including overnight spans), target playlists, shuffle toggle, and startup delay. Falls back to the current queue if no rule matches.
* **Web Audio Extractor & Downloader:** Paste any webpage URL to extract downloadable audio links via Jsoup/OkHttp and save them directly to `Downloads/FD Player`.
* **4-Mode Theme Engine:** Dark, Light, System, and Custom themes with 8 curated color palettes and automatic contrast enforcement (`ensureContrast`).
* **Trilingual Support:** Persian (default), Arabic, and English with full RTL/LTR support and per-app locale switching.

> **Important Note for Bluetooth Autoplay:**
> To ensure reliable Bluetooth auto-playback in the background, set **FD Player** battery optimization to **Unrestricted** in Android System Settings.

---

## مجوزها (Permissions)

* `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`: خواندن فایل‌های صوتی حافظهٔ دستگاه
* `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: اجرای پایدار سرویس پخش و بلوتوث در پس‌زمینه
* `BLUETOOTH_CONNECT`: تشخیص اتصال دستگاه‌های بلوتوث (اندروید ۱۲ به بالا)
* `RECORD_AUDIO`: اختیاری؛ صرفاً در صورت فعال‌سازی دستی «ریتم از میکروفون» برای واکنش بصری لاوا‌لمپ
* `INTERNET` & `ACCESS_NETWORK_STATE`: استخراج و دانلود فایل‌های صوتی از صفحات وب
* `WAKE_LOCK`: جلوگیری از توقف سرویس حین پخش و دانلود

---

## مراحل بیلد (GitHub Actions CI)

این پروژه برای بیلد خودکار در **GitHub Actions** پیکربندی شده است:
1. کدها را در یک ریپازیتوری گیت‌هاب Push کنید.
2. وارد تب **Actions** شوید؛ ورک‌فلو `Build Android APK` به‌صورت خودکار اجرا می‌شود (یا از دکمهٔ `Run workflow` استفاده کنید).
3. پس از پایان بیلد، فایل نصب (`FD-Player-v1.0.0-debug.apk`) از بخش **Artifacts** قابل دانلود است.
