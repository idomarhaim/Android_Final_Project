# `#61` — the GoalPilot calendar, proven in Google Calendar's own UI

**2026-08-23**, session `61-google-calendar`, on `emulator-5554` (`Pixel_10_Pro_XL`), signed in as
`name.iddo@gmail.com` with the `calendar.app.created` scope granted by Ido at the app's own consent
screen.

These are the shots [`#62`](https://github.com/idomarhaim/Android_Final_Project/issues/62) wants for
the tour: the proof has to be **Google's** UI, not GoalPilot's, or it only shows the app agreeing
with itself.

| File | What it proves |
|---|---|
| `goalpilot-calendar-surface.png` | The **input**: §4.3's surface (`#60`) showing three `DEADLINE` occurrences on Mon 24 Aug, *due 20:00*. This is what the sync was asked to mirror. |
| `google-calendar-banners.png` | The **output**, in Google Calendar: three orange **all-day banners** on Mon 24, `Due 20:00 · Write the project book chapter`, sitting above the timed events. Taken **after a second sync** — still three, not six. |
| `google-calendar-event-detail.png` | The **attribution**: one banner opened. `Tomorrow` (all-day, no slot), calendar **GoalPilot**, account `name.iddo@gmail.com`. |

## What each shot settles

**§2.7's deadline rule, exactly as written.** *"A `DEADLINE` is an all-day banner titled `Due 23:59 ·
Submit report`"*, on one criterion — *"the Google event does not remind … so its only job is to be
**seen**, which a banner does and a 23:59 marker does not."* The detail shot says **Tomorrow** with
no time, which is what makes it a banner rather than a 20:00 event occupying a slot the app cannot
check.

**The calendar is Ido's, and it is a secondary one.** The detail shot names **GoalPilot** under his
own address — §2.6's *"the calendar is Ido's, not the app's … created client-side; a service-account
owner is actively wrong."* Its id is a `@group.calendar.google.com` secondary calendar, and his
other calendars sit beside it untouched.

**The push is idempotent, and this is the shot that proves it.** `google-calendar-banners.png` was
taken after **two** foreground syncs. The pull was throttled on the second (§2.7's fifteen minutes;
the stored stamp was unchanged), so `remote` was empty and the push ran under
`UnknownRemote.ASSUME_STALE` — which patches a linked occurrence and inserts an unlinked one. Three
banners rather than six is therefore a direct observation that `googleEventId` was **stored** on the
first run. Had `link()` failed, this is the shot that would show six.

**Everything else in the frame is Ido's own data, and GoalPilot cannot see any of it.** The Hebrew
events around the banners come from his other calendars, rendered by Google Calendar. The app holds
`calendar.app.created` alone, which reaches only calendars it created itself — so its blindness to
those rows is structural, not a filter it applies.
