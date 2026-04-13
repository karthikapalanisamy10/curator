# Enable Google Directions API

Follow these steps to enable the Directions API for your Google Cloud project:

## Step 1: Open Google Cloud Console
1. Go to https://console.cloud.google.com
2. Sign in with the same Google account you used to create the API key

## Step 2: Select Your Project
1. Look at the top of the page where it says "Google Cloud"
2. Click on the **project dropdown** (near the top-left)
3. If you see your project name, click it
4. If not, you may need to create a new project or find the existing one

## Step 3: Navigate to APIs & Services
1. In the left sidebar, click **"APIs & Services"**
2. Click **"Library"** from the submenu

## Step 4: Search for Directions API
1. In the search box at the top, type: **"Directions API"**
2. Click on the result "Maps SDK for Android" or "Directions API"
3. Click the **"ENABLE"** button

## Step 5: Wait for Activation
⏳ **Important:** Wait 1-5 minutes for the API to fully activate

## Step 6: Verify It's Enabled
1. You should see a blue "API enabled" message
2. Go back to **APIs & Services** → **Enabled APIs & services**
3. Look for "Directions API" in the list - it should show as enabled

## Step 7: Test in the App
1. Open the CURATOR app on your device
2. Go to Schedule page
3. Enter a city name (e.g., "Madurai") in the destination field
4. Click **"Show"**
5. You should now see a **blue line** showing the route from your location to the destination

## Troubleshooting

**If it still shows REQUEST_DENIED after 5 minutes:**
- Open the app and check the logcat again
- The response should change from `REQUEST_DENIED` to `OK`
- If still denied, check if:
  - The API key in the code matches your Google Cloud project
  - Directions API is listed in "Enabled APIs & services"
  - Your Google Cloud project has billing enabled (may be required)

## Your Current API Key
```
AIzaSyB9hoqndl5t9AfLoglZIM2iykpL2CKlDa0
```

This key already works for:
- ✅ Maps Display
- ✅ Geocoding (finding city locations)
- ❌ Directions (needs to be enabled)

Once you enable Directions API, it will automatically work with this same key - no code changes needed!
