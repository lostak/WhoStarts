# App content answers

Play Console path: **Policy and programs > App content**. Answers are the same for WhoStarts and WhoStarts Pro unless a line says otherwise.

These answers match the code as of this kit: no internet permission, no ads SDK, no analytics, data saved only on the phone. When banner ads go into the free app, switch to the answers in `launch/ads-addendum.md`.

## Privacy policy

```
https://lostak.github.io/WhoStarts/privacy.html
```

Works once GitHub Pages is on (step 2 of the checklist). The in-app menu link points at the copy in `PRIVACY.md`, which says the same thing.

## Ads

- **Does your app contain ads?** No

## App access

- **All functionality is available without special access.** No sign-in, nothing locked.

## Content rating (IARC questionnaire)

- **Email for the rating certificate:** your developer email
- **Category:** All Other App Types (it's a scorekeeper, not a game)
- **Violence:** No
- **Fear / horror:** No
- **Sexuality / nudity:** No
- **Language (profanity):** No
- **Controlled substances (drugs, alcohol, tobacco):** No
- **Crude humor:** No
- **Gambling or simulated gambling:** No. The coin flip and wheel only pick who breaks; there is no betting, wagering, money or prizes.
- **Does the app let users interact or exchange content with each other?** No
- **Does the app share the user's current physical location?** No
- **Does the app allow users to purchase digital goods?** No (Pro is paid up front, not an in-app purchase)
- **Is this a web browser or search engine?** No
- **Is the app primarily a news or educational product?** No

Expected result: Everyone / PEGI 3 / USK 0.

## Target audience and content

- **Target age groups:** 13–15, 16–17, and 18 and over
- **Could the store listing unintentionally appeal to children?** No
- Why not under 13: selecting any under-13 group puts the app in Google's Families program, which restricts which ad networks the free app can use later.

## Data safety

Play Console path: **App content > Data safety**

1. **Does your app collect or share any of the required user data types?** No
2. That's the end of the form. The listing will show "No data collected" and "No data shared with third parties".

Why "No" is accurate:
- The app has no `INTERNET` permission (see `app/src/main/AndroidManifest.xml`), so nothing can leave the phone from the app itself.
- Team names, player names, colors and results are saved in the app's private storage on the device. Play only counts data as "collected" when it's sent off the device.
- `android:allowBackup="true"` lets Android's own backup copy the app data to the user's Google account if they have device backup turned on. That goes to the user's account, not to you, and the privacy policy says so.

## Advertising ID

Play Console path: **App content > Advertising ID**

- **Does your app use advertising ID?** No

## Other declarations

- **Government apps:** No
- **Financial features:** My app doesn't provide any financial features
- **Health apps:** My app does not have any health features
- **News apps:** No
- **Data deletion (account deletion):** not asked, since the app has no accounts
