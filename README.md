# Byhantools - GReset & Work Profile Manager

<p align="center">
  <b>أداة متقدمة لإدارة بروفايل العمل (Work Profile)، خدمات Google، وتثبيت حزمة Island المدمجة أوفلاين.</b><br>
  <i>An advanced Android utility for Work Profile provisioning, Google Services management, and offline Island 6.4.2 integration.</i>
</p>

---

## 🌟 المميزات الرئيسية (Features)

- **🏝️ دمج حزمة Island 6.4.2 بالكامل (أوفلاين - 100% Offline)**:
  - حزمة تطبيق Island مضمنة ذاتياً داخل التطبيق (`assets/island.apk`).
  - تثبيت بنقرة واحدة بدون الحاجة لإنترنت أو متجر Google Play.
  - إمكانية حفظ واستخراج ملف الـ APK يدوياً إلى مجلد التنزيلات (Downloads).

- **💼 إدارة وتجهيز بيئة العمل (Work Profile & Device Admin)**:
  - تجهيز وإعداد ملف العمل المعزول عبر `DevicePolicyManager`.
  - التحقق التلقائي مما إذا كان التطبيق يعمل داخل بيئة العمل أو البيئة الشخصية.

- **🆔 فحص وتحديد معرّفات الجهاز (Device & Google IDs)**:
  - عرض ونسخ معرّف الجهاز (Android ID).
  - كشف معرّف خدمات Google Framework ‏(GSF ID) بنقرة زر واحدة.
  - فحص ومراقبة حالة الاتصال بالإنترنت والشبكة لحظياً.

- **⚡ اختصارات وإعدادات النظام السريعة (System Shortcuts)**:
  - وصول مباشر لإعدادات Google Play Services و GSF و Google Play Store.
  - فتح إعدادات المزامنة، الحسابات المتعددة، وإعادة تعيين المعرّف الإعلاني (Advertising ID).

---

## 📱 متطلبات التشغيل (Requirements)

- **نظام التشغيل**: Android 5.0 (API 21) فما فوق (يدعم Android 14 و Android 15).
- **بيئة التطوير**: Android Studio مع Gradle 8.11+ و JDK 17.

---

## 🛠️ البناء والتثبيت (Build & Installation)

لبناء ملف الـ APK التجريبي (Debug APK):

```bash
# بناء نسخة الـ Debug
./gradlew assembleDebug
```

مسار ملف الـ APK الناتج:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 الترخيص (License)
هذا المشروع متاح للاستخدام والتطوير تحت رخصة مفتوحة المصدر.
