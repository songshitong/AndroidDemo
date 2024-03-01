
How Baseline Profiles work   需要google play支持
While developing your app or library, consider defining Baseline Profiles to cover common user interactions 
where rendering time or latency are important. Here's how they work:

1 Human-readable profile rules are generated for your app and compiled into binary form in the app. 
You can find them in assets/dexopt/baseline.prof. You can then upload the AAB to Google Play as usual.

2 Google Play processes the profile and ships it directly to users along with the APK. During installation, 
ART performs AOT compilation of the methods in the profile, resulting in those methods executing faster. 
If the profile contains methods used in app launch or during frame rendering, the user might experience faster launch times 
and reduced jank.

3 This flow cooperates with Cloud Profiles aggregation to fine-tune performance based on actual usage of the app over time.