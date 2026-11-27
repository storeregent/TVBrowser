BrowseHere Android TV Browser

Reverse Engineering — Initial Technical Findings

Reference APK: Browser TV Web – BrowseHere
Version: 8.0.770
Package Name: "com.tcl.browser"
Version Code: 830770
APK Size: Approximately 7.5 MB
SHA-256: "b86673a53463d0e4f47bdc9b67c28a7fe86e1076b10fd04c93440bd63e217fad"

Analysis Type: Static APK Analysis
Important: APK ko execute nahi kiya gaya. Findings APK ke manifest, DEX classes, resources, libraries aur embedded strings ki inspection par based hain.

---

1. Application Architecture

1. Application native Android architecture par based hai.
2. Main application class: "com.tcl.browser.Browser"
3. Main activity: "BrowserActivity"
4. Application Java-based Android code use karti hai.
5. APK mein ek "classes.dex" file mili.
6. Flutter, React Native ya Electron architecture ka evidence nahi mila.
7. APK mein koi bundled native ".so" library nahi mili.
8. Application hardware acceleration enable karti hai.
9. Android TV/Leanback launcher category manifest mein present hai.

Initial assessment: Yeh ek native Android browser hai, koi simple website wrapper nahi.

2. Browser Engine

APK mein Android ka built-in "android.webkit.WebView" use hone ka strong evidence mila.

Related classes/APIs:

- "BrowserWebView"
- "BrowserWebViewFactory"
- "WebViewClient"
- "WebChromeClient"
- "CookieManager"
- "DownloadManager"

Additional WebView APIs:

- "onShowCustomView"
- "onDownloadStart"
- "shouldInterceptRequest"

Is se browser mein website rendering, browser callbacks, downloads aur custom full-screen content handling ka architecture nazar aata hai.

Important unknowns:

- Actual Chromium/WebView runtime version.
- JavaScript configuration.
- DOM storage settings.
- Cookie persistence configuration.
- Cache strategy.
- User-Agent customization.
- HTML5 video compatibility.
- Hardware-accelerated video playback behavior.

3. Tabs and Browser Management

APK mein following classes identify hui hain:

- "Tab"
- "TabControl"
- "Controller"
- "Bookmarks"
- "BrowserActivity"

Is se tab management aur browser controller ka dedicated architecture confirm hota hai.

Browser history, bookmarks aur browser provider-related classes bhi mili hain.

Exact tab lifecycle, background tab behavior aur memory management abhi runtime testing se verify nahi hue.

4. Android TV Remote and Navigation

APK mein custom TV interaction-related classes mili hain:

- "MouseControl"
- "PieControl"
- "PieMenu"
- "PieItem"

Yeh indicate karta hai ke application mein Android TV remote ke liye custom interaction layer hai.

Possible functionality:

- Pointer/mouse navigation.
- Custom pie-style navigation menu.
- Remote-control based browser interaction.

Actual remote behavior ko Android TV device par test karna zaroori hai.

5. Networking Libraries

APK mein following networking libraries identify hui hain:

- OkHttp
- Okio
- OkGo ("com.lzy.okgo")
- Gson
- Apache HTTP legacy classes
- KSOAP2
- XML Pull Parser
- KXML2
- Android JSON APIs

In ka possible use:

- HTTP requests.
- JSON data exchange.
- SOAP web-service communication.
- XML processing.
- Server responses handling.

Yeh libraries khud paid services nahi hain. In mein se kai open-source frameworks hain.

6. TCL Backend / External Services

APK ke DEX strings mein following endpoints mile:

Browser homepage/resources:

- "http://browser.tcloudfamily.com/tclbrowser/newtab.html"
- "http://browser.tcloudfamily.com/tclbrowser/tclindex.html"
- "http://browser.tcloudfamily.com/tclbrowser/browserZip.zip"

User-related service:

- "http://tvuser-ch.cedock.com/uc/json"
- "http://webservice.uc.tcl.com/insertUser"

Browser navigation/search configuration:

- "http://browsernav.api.my7v.com/tvbase/pageSearchApi/findPageConfig?"

SOAP service paths:

- "/TCLUserCollection/services/browserWebService?wsdl"
- "/TCLUserCollection/services/favoriateWebService?wsdl"
- "/TCLUserCollection/services/userWebService?wsdl"
- "/TCLUserCollection/services/userlogWebService?wsdl"

Related classes mein "SubmitDataService" aur "LocalPageDownloadService" bhi mili hain.

Important: APK mein endpoints milna unke currently operational hone ka proof nahi. Actual requests aur responses runtime network inspection se verify karne honge.

7. Homepage and Web Assets

APK ke andar local HTML, CSS, JavaScript aur image assets maujood hain.

Identified files:

- "assets/newtab.html"
- "assets/tclindex.html"
- "assets/tclindex_small_icon.html"
- "assets/css/golbal.css"
- "assets/css/header.css"
- "assets/html/"
- "assets/images/"

Is se pata chalta hai ke homepage aur kuch browser UI content HTML/CSS/JavaScript ke through implement kiya gaya hai.

New-tab JavaScript mein obfuscated/eval-packed code bhi mila.

Possible architecture: Native Android browser shell + WebView + HTML/CSS/JavaScript based homepage.

8. Voice Search

APK mein following Android speech recognition references mili hain:

- "startVoiceRecognizer"
- "android.speech.action.RECOGNIZE_SPEECH"
- "android.speech.extra.RESULTS"

Is se Android speech recognition integration ka evidence milta hai.

Lekin yeh verify nahi hua ke voice recognition kis provider ke through chalti hai, ya kisi external paid API ki dependency hai.

9. Video Playback

APK mein following evidence mila:

- Android WebView.
- WebChromeClient.
- "onShowCustomView".
- Custom full-screen content handling.

Lekin bundled video-player frameworks ka evidence nahi mila:

- ExoPlayer
- AndroidX Media3
- IJKPlayer
- Vitamio

Koi bundled native video-player library bhi identify nahi hui.

Initial assessment: WebView aur Android system capabilities ke through video playback ka possibility hai. Actual playback compatibility aur codec support device-level testing se verify karna hoga.

10. Local Database and Storage

APK mein browser-related local data architecture ka evidence mila:

- BrowserProvider
- SQLite-related schema
- Browser history
- Bookmarks
- Preferences/settings

Is se local browser data management ka implementation nazar aata hai.

11. Android Permissions

Manifest mein following permissions mili hain:

- INTERNET
- ACCESS_NETWORK_STATE
- ACCESS_WIFI_STATE
- ACCESS_FINE_LOCATION
- ACCESS_COARSE_LOCATION
- READ_EXTERNAL_STORAGE
- WRITE_EXTERNAL_STORAGE
- GET_ACCOUNTS
- USE_CREDENTIALS
- READ_CONTACTS
- READ_PROFILE
- NFC
- WAKE_LOCK
- WRITE_SETTINGS
- READ_SYNC_SETTINGS
- WRITE_SYNC_SETTINGS
- MANAGE_ACCOUNTS
- Browser history/bookmark permissions
- Download manager-related permission
- Wallpaper
- Launcher shortcut

Important: Manifest mein permission present hona is baat ka proof nahi ke application har permission ko actively use karti hai.

Modern application mein har permission ki necessity aur privacy implications review karni hongi.

12. TCL-Specific Android Integrations

Manifest mein optional shared libraries mili hain:

- "com.tcl.tvmanager"
- "android.tclwidget"
- "com.tcl.tvos.addon"

Yeh TCL-specific TV integrations ka evidence hain.

Expert ko evaluate karna hoga ke in integrations ke baghair browser ko generic Android TV devices par kaise operate karwaya ja sakta hai.

13. APK Signing

Signing certificate mein:

- Subject: TCL
- Signature algorithm: SHA1withRSA
- Certificate validity: 2014-06-05 to 2041-10-21

Yeh old signing configuration hai.

Is certificate ko apni application mein reuse karne ki zaroorat nahi. Apni application ki independent signing key honi chahiye.

14. Ads and Paid Services — Findings

Static inspection mein:

- OkHttp/OkGo jaise networking frameworks mile.
- TCL backend endpoints mile.
- Koi confirmed paid API subscription identify nahi hui.
- Firebase/AdMob SDK strings identify nahi hui.
- Koi identifiable dedicated ad-blocking library ya EasyList implementation nahi mili.
- "AD.jpg" naam ka ek asset bhi APK mein hai, lekin yeh apne aap mein active advertising system ka proof nahi.

Conclusion: Is APK se kisi mandatory paid third-party API ya subscription ki confirmation nahi hoti. TCL backend ka operational aur financial model unknown hai.

---

15. Technical Configuration

Property| Finding
Package| "com.tcl.browser"
Version| 8.0.770
Version Code| 830770
Min SDK| 14
Target SDK| 23
Platform build name| 5.1.1-1.0.0
Platform build code| 22
Hardware acceleration| Enabled
Native ".so" libraries| None found
DEX files| One
Leanback launcher| Present

Yeh configuration old Android generation ki hai. Is architecture ko modern Android TV devices par directly reproduce karna bina compatibility testing ke recommend nahi kiya ja sakta.

---

Mai Android TV k liye Browser Bnana chahta hoon oper mene Android TV k sab sy best browser ki Reverse Engineering ki Information Paste ki hai.

Mere Kuch Questions hain Ap ny questions ko aesy under stand krna or answer krna hai keh hamara goal hai is sy better Browser Bnana.

Here's My Questions List:

A. Architecture

1. Kya BrowseHere ka native Android + WebView architecture modern Kotlin mein reproduce karna behtar hai?
2. Kya Android System WebView sufficient hai ya custom Chromium engine ki zaroorat hogi?
3. Kya WebView engine ko future mein update karna easy hoga?

B. Android TV Experience

4. Remote-control navigation aur mouse pointer ka best architecture kya hoga?
5. Kya custom Pie Menu ki zaroorat hai?
6. Android TV ke different manufacturers aur remotes ko support kaise kiya jaye?

C. Video Playback

7. Kya WebView HTML5 video aur full-screen playback ke liye sufficient hai?
8. Kya Media3/ExoPlayer integrate karna chahiye?
9. Different codecs, DRM aur streaming formats ko kaise handle karna chahiye?

D. Browser Features

10. Multiple tabs ka efficient architecture kya hoga?
11. Bookmarks, history aur downloads ke liye best local database/storage approach kya hai?
12. Ad blocking implement karne ka reliable tareeqa kya hoga?
13. Incognito mode aur private browsing kaise implement honi chahiye?

E. Backend and Costs

14. Kya browser ko completely offline/local architecture par develop kiya ja sakta hai?
15. Search engine, voice search aur homepage ke liye paid APIs zaroori hongi?
16. Kya apna lightweight backend banana chahiye?
17. TCL ke backend integrations ke alternatives kya honge?

F. Security and Compatibility

18. Old APK ki permissions mein se kaun si unnecessary hain?
19. HTTPS, certificate handling, cookies aur browsing data ko secure kaise rakha jaye?
20. Modern Android TV versions aur Google TV devices par compatibility kaise ensure ki jaye?

---