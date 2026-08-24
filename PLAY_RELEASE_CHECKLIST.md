# Google Play release checklist

The app version remains `beta1`. No Android App Bundle has been generated.

## Completed in the repository

- [x] Release signing values are read from ignored `keystore.properties`.
- [x] Apache License 2.0, notice, and project README are present.
- [x] Unused Room, networking, location, Accompanist, AppCompat, legacy navigation, icon, and KSP dependencies were removed.
- [x] The dependency-cleaned merged manifest does not contain the `INTERNET` permission.
- [x] Camera-in-use and missing-flash states are handled without crashing.
- [x] Essential controls have accessible labels and at least 48 dp interaction height.
- [x] A public-website-ready privacy policy is available at `privacy-policy.html`.

## Play Console Data safety answers

Verify these against the final release build before submission:

- Data collected or shared: **No**.
- Data encrypted in transit: **Not applicable; the app does not transmit user data**.
- Account creation: **No**.
- Account deletion: **Not applicable; the app has no accounts or server-side user data**.
- Ads: **No**.
- Analytics or tracking: **No**.

The app stores preferences locally. Android may back them up when the user has device backup enabled; this is disclosed in the privacy policy.

## Required after the repository is public

- [ ] Host `privacy-policy.html` at a stable, public, non-geofenced HTTPS URL.
- [ ] Add that URL to Play Console and expose the same policy from the app or its store-facing support flow.
- [ ] Complete the Play Console Data safety form using the answers above.
- [ ] Complete content rating, target audience, app access, ads, and store-listing declarations.
- [ ] Run an internal testing release and review the Play pre-launch report on physical device models.
- [ ] Generate the AAB only when the release candidate is approved.
