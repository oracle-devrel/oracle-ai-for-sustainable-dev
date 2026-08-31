# Agent-memory walkthrough scene plan

The video follows the exact 16-step story below. Each step shows the live
application, the source method invoked, and database evidence. It is silent so a
presenter can add narration later; burned-in text and matching SRT and WebVTT
captions provide the complete explanation.

| Step | Scenario and visible evidence | Source methods | Database proof |
|---|---|---|---|
| 1 | Begin Ava's visit; retain quiet, accessible, and entertainment preferences. | `retain()` | Added message and embedded memory rows. |
| 2 | Recall and reuse Ava-scoped context for a personalized plan. | `recall()` | Scoped, ranked reads; no row changes. |
| 3 | Correct fireworks to the lantern show. | `correct()` | Memory content and provenance updated. |
| 4 | Expire the temporary garden-path closure. | `expire()` | TTL state updated; expired row excluded from recall. |
| 5 | Privacy-gate three synthetic rainy-route outcomes and propose a pending lesson. | `dream()` | Pending guideline added only after scope and identifier checks. |
| 6 | Approve the candidate as Ava's organizer identity. | `approve_guideline()` | Deep Data Security authorizes the metadata update. |
| 7 | Help Leo and run the cross-user attack probe. | `_rows_for()` and `next_day()` | Participant data grant returns one approved guideline, zero Ava rows, and zero raw traces. |
| 8 | Reset quest results while preserving the park world. | `reset()` | Progress, badges, and reward audit cleared. |
| 9 | Plan a covered, step-free route on the map. | `plan()` | Property Graph and Spatial reads; no row changes. |
| 10 | Form the consent-bounded party and start the quest. | `start()` | Progress and `QUEST_STARTED` audit inserted transactionally. |
| 11 | Reach Quiet Cafe and earn 50 points. | `complete_next_step()` | Progress and reward audit updated atomically. |
| 12 | Reach Covered Atrium and earn another 50 points. | `complete_next_step()` | Ordered progress and second audit event committed. |
| 13 | Finish at Lantern Garden with 400 points and a badge. | `complete_next_step()` | Completion, badge, and final audit committed together. |
| 14 | Retrieve vector-ranked lore and expand it through park relationships. | `graph_rag()` | Knowledge and graph tables read; no row changes. |
| 15 | Start a private AR session and remember a confirmed observation. | `start_session()`, `remember()` | Session, expirable memory, and AR audit rows added. |
| 16 | Index and search a consented media description. | `remember_media()`, `search_media()` | Scoped media vector and audit evidence added, then ranked. |

The opening and closing visuals summarize the connected architecture and the
Four Rs. `build-video.swift` deterministically generates the MP4, poster, and
caption sidecars from the current source and verified captures.
