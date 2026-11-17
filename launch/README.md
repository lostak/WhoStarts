# WhoStarts launch kit

Everything needed to put WhoStarts (free) and WhoStarts Pro (paid) on Google Play. Every step below works from a phone: Play Console runs in Chrome (use "Desktop site" if a page looks cramped), and GitHub works in the GitHub app or the mobile site.

| File | What it's for |
|---|---|
| `listing-free.md` | Name, descriptions, graphics list and store settings for WhoStarts |
| `listing-pro.md` | The same for WhoStarts Pro, plus pricing |
| `app-content.md` | Answers for every App content form: Data safety, content rating, target audience, ads |
| `ads-addendum.md` | What to change when banner ads go into the free app |
| `graphics/` | 512px icon, feature graphics, captioned phone screenshots |
| `../docs/privacy.html` | Privacy policy page for GitHub Pages |

## Where the build stands

- **Package names:** `com.lostak.whostarts` (free) and `com.lostak.whostarts.pro` (Pro)
- **Target SDK:** 36 (Android 16), which meets Play's current requirement for new apps
- **Signed app bundles:** the "Build APK" GitHub Action already builds `app-free-release.aab` and `app-pro-release.aab`, signed with the upload key in `signing/`. The latest ones are from run #24 (Oct 23) and stay downloadable until Jan 21, 2027. Any push to `master` makes fresh ones.
- **Version code:** taken from the Action's run number, so always upload bundles built by the Action, never a local build.
- **Permissions:** none. No internet, no ads SDK, no analytics.
- **Privacy link inside the app:** already in the top-right menu.

## Release blockers

1. **Pro has nothing over the free app yet.** The free build has a `SHOW_ADS` flag but no ads SDK, so today both apps are the same app and one costs money. Play can reject that as repetitive content, and buyers would get nothing extra. Launch the free app first; launch Pro in the same update that adds the banner (see `ads-addendum.md`).
2. **The privacy policy URL needs GitHub Pages switched on** after this branch is merged (step 2 below). Until then the Play form has no public page to point at.

Nothing else in the code blocks a release.

Small things that don't block launch: both apps use the same launcher icon (a Pro badge would help people who install both), code shrinking is off (the app is small, so it only costs a few MB), and the code namespace is still `com.example.pooltracker` (internal only; Play only sees the package names above).

## Checklist

### 1. Merge

- [ ] Merge this branch into `master`. That also triggers a fresh signed build.

### 2. Turn on the privacy policy page

- [ ] GitHub repo > **Settings > Pages**
- [ ] Source: **Deploy from a branch**, branch **master**, folder **/docs**, Save
- [ ] After a minute, open `https://lostak.github.io/WhoStarts/privacy.html` and check it loads

### 3. Get the app bundle

- [ ] GitHub repo > **Actions > Build APK**, open the newest green run
- [ ] Under Artifacts, download **whostarts-release-aab** (it arrives as a zip)
- [ ] In the Files app, tap the zip and extract it to get `app-free-release.aab`

### 4. Create the free app in Play Console

- [ ] **Create app**: name `WhoStarts: Pool Game Tracker`, language English (United States), **App**, **Free**, accept the declarations
- [ ] Main store listing: paste from `listing-free.md`, upload the icon, feature graphic and screenshots
- [ ] Store settings: category **Sports**, tags, contact email
- [ ] App content: fill every form from `app-content.md` (privacy policy, ads, app access, content rating, target audience, data safety, advertising ID, government, financial, health, news)

### 5. Closed test (required for new personal accounts)

Play requires personal developer accounts created after November 13, 2023 to run a closed test with **at least 12 testers who stay opted in for 14 days in a row** before production is unlocked. Play applies this to each new app. Testers who opt in and then drop out before 14 days don't count.

- [ ] **Test and release > Testing > Closed testing**, create a track
- [ ] Testers: add an email list (or a Google Group) with 12 or more people. Aim for 15 or so in case a few never opt in
- [ ] Create a release, accept **Play App Signing** when asked (Google keeps the app signing key, your upload key stays in this repo), upload `app-free-release.aab`, release notes: "First test build"
- [ ] Pick countries, send for review
- [ ] Once approved, share the opt-in link. Each tester must tap **Become a tester** and install from Play
- [ ] Ask testers to open the app a few times over the 2 weeks. Play asks about tester engagement when you apply
- [ ] Keep pushing fixes to the same track if needed. New builds don't reset the 14-day clock

### 6. Apply for production

- [ ] After 14 days with 12+ testers opted in: **Dashboard > Apply for production**
- [ ] Answer the three sections: about your closed test (how you found testers, how engaged they were, what feedback you got and what you changed), about the app (who it's for: pool players tracking games with friends), production readiness
- [ ] Review usually takes 7 days or less. Play emails you the result

### 7. Release

- [ ] **Production > Create new release**, promote the tested bundle (or upload the latest one)
- [ ] Countries: start with all where you're comfortable answering support email
- [ ] Optional: staged rollout at 20% for a couple of days, then 100%

### 8. WhoStarts Pro (after the banner ads ship)

- [ ] Set up a **payments profile** (Settings > Payments profile) if you haven't, so you can sell paid apps
- [ ] **Create app** `WhoStarts Pro: Pool Tracker`, **Paid**. Set the price (suggested $1.99) **before the first release on any track**; Play never lets a free app become paid
- [ ] Listing, graphics and pricing from `listing-pro.md`, App content from `app-content.md` (Pro keeps the no-ads answers)
- [ ] Pro needs its own closed test (12 testers, 14 days). You can reuse the same tester list
- [ ] Bundle: `app-pro-release.aab` from the **whostarts-pro-release-aab** artifact
- [ ] Apply for production and release, same as the free app
