# The marketing film — cut plan and narration

**File** `C:\Users\namei\Videos\GoalPilot-Tour\GoalPilot-marketing-film.mp4`
**Format** 1080×1920, 9:16, 30 fps · **Length** 2:10 · **Sources** all in `…\Videos\GoalPilot-Tour\`.

## Why the app footage is sped up

`record-tour.sh` holds every beat for `HOLD=3.0 s` / `HOLD_LONG=4.5 s` **on purpose**, so a narrator
can talk over a still screen. In a two-minute marketing film those holds are dead air — Ido's note,
and he was right: *"this way you show almost nothing."* The app segments therefore run at **1.8×**,
which buys room for **challenges** and **theming** without making the film longer. 144 s of source
becomes 80 s of screen time, and the feature count goes from six to eight.

## Structure

| # | segment | source | in → out | len | at |
|---|---|---|---|---|---|
| 1 | laptop closes · dusty shoes · cased saxophone | `opening-broll-seedance25` | `0:00 → 0:12` | 12.0 | `0:00` |
| 2 | the over-full calendar | `opening-broll-calendar-patch` | `0:00 → 0:05` | 5.0 | `0:12` |
| 3 | the phone is picked up | `opening-broll-seedance25` | `0:16 → 0:20` | 4.0 | `0:17` |
| 4 | **smart add** — a sentence, read, filed, shown before it saves | tour `0:23.0` +22 s | 1.8× | 12.2 | `0:21` |
| 5 | the AI estimate, the date, the deadline, added | tour `2:35.5` +32.5 s | 1.8× | 18.1 | `0:33` |
| 6 | goals as time, and the load bar | tour `3:21.5` +13.5 s | 1.8× | 7.5 | `0:51` |
| 7 | drag it where it should have been | tour `4:03.0` +15 s | 1.8× | 8.3 | `0:59` |
| 8 | the parts of your life | tour `4:30.3` +11.7 s | 1.8× | 6.5 | `1:07` |
| 9 | **challenges between friends** | tour `7:21.0` +9 s | 1.8× | 5.0 | `1:14` |
| 10 | where your time actually went | tour `5:33.0` +22 s | 1.8× | 12.2 | `1:19` |
| 11 | **four materials — make it yours** | tour `8:48.0` +18 s | 1.8× | 10.0 | `1:31` |
| 12 | payoff — the shoes go out | `payoff-shoes` | full | 8.0 | `1:41` |
| 13 | payoff — the saxophone is played | `payoff-sax` | full | 8.0 | `1:49` |
| 14 | payoff — the evening comes back | `payoff-evening` | full | 8.0 | `1:57` |
| 15 | closing card | ffmpeg `drawtext` | — | 5.0 | `2:05` |

### Privacy

⚠️ The tour's privacy window is `6:54.1` → `7:55.2` — a friend's real name on the leaderboard, then
the email address and friend code on the profile. **Segment 9 sits inside that window and is still
safe**, because it was checked frame by frame rather than by the timecode: the *Challenges* screen
shows only `August Steps Race · 2 people in · Open-ended · #2 · 0 steps · Report score · Standings`.
No friend name, no email, no friend code. **Every other segment is outside the window entirely.**

Ido cleared the full tour for course submission; the marketing film is the one that might travel,
so it carries none of it.

## Narration — timed to the cut above

**`0:00`–`0:21` · the problem**

> There is always a list.
> The run you were going to start. The instrument you were going to pick back up.
> They do not fail loudly. They just never get a time.

**`0:21`–`0:33` · smart add**

> So start with a sentence. Say it the way you'd say it to a friend.
> GoalPilot reads it, files it under the right goal, and shows you where it went before it saves.

**`0:33`–`0:51` · the estimate**

> It works out how demanding it is, how long it takes, what it's worth —
> and puts it on a day, at an hour, as a real commitment.

**`0:51`–`1:07` · the calendar**

> Now it isn't a list. It's time. With the load you're actually carrying.
> Missed one? Pick it up and put it where it should have been.

**`1:07`–`1:14` · life areas**

> Every goal belongs to a part of your life you decided was worth investing in.

**`1:14`–`1:19` · challenges**

> And some of them go better with someone else in the race.

**`1:19`–`1:31` · analytics**

> Then the honest answer. Not a score, not a grade —
> just where your time actually went, including the part that was never filed at all.

**`1:31`–`1:41` · theming**

> And it looks the way you want to look at it.

**`1:41`–`2:10` · the payoff**

> The list never needed more discipline.
> It needed a time.
> **GoalPilot. Give it a time.**

## Voice notes

- One voice, warm, unhurried. Not an ad-read.
- The problem and payoff lines want space; the product lines can run tighter.
- ⚠️ Never call `#64`'s run a **score**, a **rate**, or describe it as **red**. The narration above
  says *"Not a score, not a grade"* deliberately.

## Where the voiceover comes from

**Not from the MCP.** Verified against the full roster: all 17 models expose only `image` and
`video` modes, and the connector has exactly two generators — `openart_generate_image` and
`openart_generate_video`. There is **no `generate_audio` and no Director tool**. Audio appears only
as an *upload input* (`openart_upload_sign` accepts `mediaType: audio`).

So the voice is generated in **OpenArt's Audio tab in the browser** and muxed in here, or the whole
cut is handed to **Director** with the prompt below.

---

## ✅ The narrated film — `GoalPilot-marketing-film-narrated.mp4`

**1080×1920 · 2:10 · 23 MB.** Voiceover generated by Ido in OpenArt Director
(**MiniMax Speech 2.8 Turbo**, voice *Expressive Narrator*, British male, speed 1.00) and muxed
here by [`scripts/mux-voiceover.sh`](../../scripts/mux-voiceover.sh).

**The file→line mapping was verified, not assumed.** The nine mp3s are oldest-first by their
generation timestamp; Director lists them newest-first. Every duration matches across the two
orderings (`6.52 · 2.22 · 7.37 · 3.02 · 4.31 · 8.27 · 8.79` ↔ `0:07 · 0:02 · 0:07 · 0:03 · 0:04 ·
0:08 · 0:09`), which is what pins the mapping.

| # | line | length | placed at | measured speech |
|---|---|---|---|---|
| 1 | There is always a list… | 10.24 | `0:02` | 2.17 → 11.98 |
| 2 | So start with a sentence… | 9.00 | `0:22` | 22.03 → ~31 |
| 3 | It works out how demanding… | 8.79 | `0:35` | 34.98 → 43.41 |
| 4 | Now it is not a list… | 8.27 | `0:53` | 53.02 → 60.85 |
| 5 | Every goal belongs… | 4.31 | `1:08` | 67.89 → 71.92 |
| 6 | And some of them go better… | 3.02 | `1:15` | 74.94 → 77.51 |
| 7 | Then the honest answer… | 7.37 | `1:21` | 80.91 → 87.86 |
| 8 | And it looks the way… | 2.22 | `1:34` | 93.86 → 95.65 |
| 9 | The list never needed… | 6.52 | `1:56` | 118.80 → ~125.5 |

Clip 9 starts at `1:56` **on purpose**: it runs 6.5 s, so *"Give it a time"* lands on the closing
card at `2:05`.

⚠️ **The first mix CLIPPED and nothing said so.** Voice at ×1.6 over the ducked ambient gave
`max_volume: 0.0 dB` — full scale, distorting on every peak. `ffmpeg` exited 0 and the file played.
Fixed by dropping the voice to ×1.25 and adding `alimiter=limit=0.89`: now `max_volume: -1.5 dB`,
mean `-19.4 dB`. **`volumedetect` is the check** — a mix is never verified by listening to whether
it "sounds loud enough".

Placement was verified with `silencedetect` against the intended offsets rather than trusting the
exit code, which is the same discipline that caught the four silent renames and the truncated take.
