# When banner ads go into the free app

The free build has a `BuildConfig.SHOW_ADS` flag but no ads SDK yet. When the AdMob banner is added, these must change in the **same release**, before it goes out. WhoStarts Pro changes nothing.

## 1. Privacy policy

In `docs/privacy.html` and `PRIVACY.md`, replace the "Ads" section with:

> **Ads (free WhoStarts app only).** The free WhoStarts app shows banner ads served by Google AdMob. To show and measure ads, the Google Mobile Ads SDK collects your device's advertising ID, approximate location derived from your IP address, and app interaction and diagnostic data, and shares it with Google. Google's use of this data is described at https://policies.google.com/technologies/partner-sites. You can reset or delete your advertising ID in Android Settings > Privacy > Ads. WhoStarts Pro shows no ads and collects nothing. The game data you enter is never shared with the ad network.

Also delete the sentence "It has no accounts, no analytics, and no tracking" and update "Last updated".

## 2. Free listing text

In `launch/listing-free.md`, change "No account, no tracking, works with no signal" to "No account, and it works with no signal". Ads still need a connection to load, but the app works offline.

## 3. App content (free app only)

- **Ads:** Yes, my app contains ads
- **Advertising ID:** Yes, used for Advertising or marketing (AdMob)
- **Data safety:** Yes, the app collects data. Declare what the Google Mobile Ads SDK collects, as listed in Google's own guide (https://developers.google.com/admob/android/privacy/play-data-disclosure). At the time of writing that guide lists:
  - Location: approximate location (collected, shared, for advertising and analytics)
  - App activity: app interactions (collected, shared, advertising, analytics, fraud prevention)
  - App info and performance: crash logs and diagnostics (collected, shared, analytics, fraud prevention)
  - Device or other IDs: advertising ID (collected, shared, advertising, analytics, fraud prevention)
  - Data is encrypted in transit: Yes
  - Users can request deletion: No (there's nothing of theirs on your side to delete)
  - Recheck that guide on the day you fill this in, since Google updates it.

## 4. Code

- Add the `com.google.android.gms:play-services-ads` dependency to the `free` flavor only, so Pro never ships the SDK.
- Put the AdMob app ID in the free flavor's manifest.
- Show the banner only when `BuildConfig.SHOW_ADS` is true.
- Add the `INTERNET` permission (the ads SDK merges it in on its own).
- Use AdMob's test ad unit IDs until the store listing is live, then switch to the real banner unit ID.
- For users in the EEA and UK, AdMob requires a consent message: set up the Privacy & messaging GDPR message in AdMob and call the User Messaging Platform SDK at launch.
