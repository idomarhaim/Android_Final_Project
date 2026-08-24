#!/usr/bin/env bash
# Mux the nine OpenArt voiceover clips onto the marketing film at measured offsets.
#
# MAPPING was verified, not assumed: the files are oldest-first by their generation
# timestamp, Director listed them newest-first, and every duration matches across the
# two orderings (6.52/2.22/7.37/3.02/4.31/8.27/8.79 <-> 0:07/0:02/0:07/0:03/0:04/0:08/0:09).
#
# The film's own audio (ambient room tone from the generated shots) is ducked to 0.22
# and the voice sits on top at full level. amix normalize=0 keeps it from dropping the
# whole mix when a clip starts.
set -u
FF="/c/Users/namei/AppData/Local/Programs/ffmpeg/bin/ffmpeg"
VO="/c/Users/namei/Videos/GoalPilot-Tour/Voice Overs/test1"
FILM='C:\Users\namei\Videos\GoalPilot-Tour\GoalPilot-marketing-film.mp4'
OUT='C:\Users\namei\Videos\GoalPilot-Tour\GoalPilot-marketing-film-narrated.mp4'

# oldest -> newest == script order 1..9
mapfile -t F < <(ls -1 "$VO"/*.mp3 | sort)
[ "${#F[@]}" -eq 9 ] || { echo "expected 9 mp3s, found ${#F[@]}"; exit 1; }

# start time in MILLISECONDS for each block, per the cut plan
MS=(2000 22000 35000 53000 68000 75000 81000 94000 119000)

INPUTS=()
for f in "${F[@]}"; do INPUTS+=(-i "$(cygpath -w "$f")"); done

FC="[0:a]volume=0.22[bg];"
MIX="[bg]"
for i in "${!F[@]}"; do
  n=$((i+1))
  FC+="[${n}:a]adelay=${MS[$i]}|${MS[$i]},volume=1.25[a${n}];"
  MIX+="[a${n}]"
done
FC+="${MIX}amix=inputs=10:duration=first:normalize=0[mixed];[mixed]alimiter=limit=0.89:level=disabled[aout]"

MSYS_NO_PATHCONV=1 "$FF" -v error -y -i "$FILM" "${INPUTS[@]}" \
  -filter_complex "$FC" \
  -map 0:v -map "[aout]" -c:v copy -c:a aac -b:a 192k -ar 48000 -ac 2 \
  "$OUT"
echo "EXIT=$?"
