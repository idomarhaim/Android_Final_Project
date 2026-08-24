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
