# Google Play — Data safety & App content answers

Answers for **Play Console → Policy → App content**, based on what the app's code actually does: no analytics, ads or crash-reporting SDKs; network traffic goes only to the user-configured Immich server; permissions are `INTERNET` and `ACCESS_NETWORK_STATE`. Re-check them if the app gains a dependency or a network destination.

## Data safety

**Data collection and security → "Does your app collect or share any of the required user data types?"**
→ **No.**

Choosing **No** finishes the form. The encryption and deletion questions only appear after a **Yes**. The store listing then shows **"No data collected"** and **"No data shared with third parties"**.

### Why "No" (keep this in case review asks)

Google defines *collect* as "transmitting data from your app off a user's device". Each data type the app touches:

| What | Where it goes | Why it isn't declared |
|---|---|---|
| Photos & videos | **Downloaded** from the user's own server to display them; never uploaded | Inbound data isn't collection, and the app has no upload path (read-only, enforced in code) |
| Immich API key | Sent only to the server address the user entered | The user's own server, set up by the user. Same situation as Google's FAQ on uploads to the user's own Drive or Dropbox: no declaration when the data goes directly to the user's own account and the developer never accesses it |
| Settings, encrypted API key, image cache | Stored on the device only | On-device processing is exempt |
| Diagnostics, crash logs, app activity | None: no SDKs, and logs only in debug builds | — |
| Device or advertising IDs | Not accessed | — |
| IP address | Seen by the user's own server as part of any connection | The developer receives nothing; no location use |

**Sharing:** none. The developer has no backend and no third-party SDKs.

**Judgment call:** Google's FAQ doesn't address a server the user configures, only the user's own Drive or Dropbox. If a reviewer disagrees, the conservative fallback is to declare **Personal info → User IDs** (the API key identifies the user's account on their server), with:
- *Collected:* Yes. *Shared:* No.
- *Processed ephemerally:* No.
- *Required:* Yes. *Purpose:* App functionality.
- *Encrypted in transit:* answer **No**, because `http://` is possible when the user allows it.
- *Users can request deletion:* Yes, via Logout or uninstalling.

## Other App content declarations

| Declaration | Answer |
|---|---|
| **Privacy policy** | `https://github.com/ale-cubero/google-tv-client-for-immich/blob/main/docs/PRIVACY.md` (works once the repo is public) |
| **Ads** | No, the app contains no ads |
| **Advertising ID** | No, the app doesn't use the advertising ID |
| **App access** | "All or some functionality is restricted" — see below |
| **Content rating** | Category **"All other app types"**. Answer **No** to violence, sexual content, language, drugs, gambling, user interaction or communication, location sharing and digital purchases. Expected result: Everyone / PEGI 3 / USK 0 |
| **Target audience** | **18 and over** (not designed for children; avoids the Families policy) |
| **News app** | No |
| **Health app** | No |
| **Financial features** | None |
| **Government app** | No |

## App access (instructions for reviewers)

The app needs an Immich server, so reviewers need working credentials, or the review fails. Fill in **App access → Add instructions** with a server you control, holding demo photos and a **read-only** API key. Never use your personal library.

> **Name:** Demo Immich server
> **Server URL:** `https://<your-demo-server>`
> **API key:** `<read-only demo key>`
> **Instructions:** Launch the app, enter the Server URL and API key on the "Connect to Immich" screen, and select Connect. Navigation uses the D-pad, OK and Back. On Home, press Down to reach the "On this day", Recent and Albums rows. Open any photo with OK; in the viewer, OK opens actions (Show details, Start slideshow).

Keep that server online and the key valid for as long as the app is listed. Google re-reviews updates.
