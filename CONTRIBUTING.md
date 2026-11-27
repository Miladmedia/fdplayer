# مشارکت در توسعه FD Player / Contributing to FD Player

از علاقهٔ شما برای مشارکت در توسعهٔ **FD Player** سپاسگزاریم!

## قوانین مشارکت
1. ریپازیتوری را Fork کنید و یک شاخهٔ جدید (`feature/your-feature` یا `fix/your-bug`) بسازید.
2. معماری **Clean Architecture + MVVM/MVI** را رعایت کنید:
   - مدل‌ها و اینترفیس‌ها در `domain/`
   - پیاده‌سازی دیتابیس، شبکه و Repository در `data/`
   - کامپوننت‌های Compose و ViewModel در `ui/`
3. تمام رشته‌های جدید رابط کاربری را هم‌زمان در سه فایل زیر با لحن محاوره‌ای و رعایت Escape کاراکترهای XML (`&amp;` و `\'`) اضافه کنید:
   - `app/src/main/res/values/strings.xml`
   - `app/src/main/res/values-fa/strings.xml`
   - `app/src/main/res/values-ar/strings.xml`
4. کدها را بدون warning یا import اضافه ارسال کنید و مطمئن شوید workflow گیت‌هاب بدون خطا پاس می‌شود.

---

Thank you for contributing to **FD Player**!

1. Fork the repository and create a new branch.
2. Follow the Clean Architecture boundaries (`domain/`, `data/`, `ui/`, `service/`).
3. Add all UI strings simultaneously to `values`, `values-fa`, and `values-ar` with proper XML escaping.
4. Ensure GitHub Actions CI passes cleanly before opening a Pull Request.
