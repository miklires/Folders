# Folders

A client-side Fabric mod that adds folders to the singleplayer, multiplayer and
resource pack screens.

Folders are a view, not a storage format. `saves/`, `servers.dat` and
`resourcepacks/` are never read for anything but identity and never written at
all; grouping lives in `config/folders/*.json`. Uninstall the mod and Minecraft
behaves exactly as it did before, with every world, server and pack where it was.

---

## Status

**The core is implemented and tested. The Minecraft-facing layer is written but
has not been compiled or run.**

That split is not an accident of effort, it is what the environment allowed:
`maven.fabricmc.net`, `meta.fabricmc.net` and `libraries.minecraft.net` are all
blocked by the network policy where this was developed, so Gradle cannot resolve
Fabric Loom or Minecraft itself, and the real 1.26.2 mappings could not be
consulted. Rather than guess quietly, the mod is arranged so the guessing is
confined to as few lines as possible.

| Layer | State |
|---|---|
| `core/` — model, storage, identity, drag, animation, layout | Compiled and unit tested, 65 tests green |
| `identity/`, `ui/`, `integration/`, `mixin/` | Written, reviewed, **not compiled** |
| Textures, translations, manifests | Present |

Before the first build, work through [Mapping checklist](#mapping-checklist).

---

## Building

```bash
./gradlew build          # jar in build/libs/
./gradlew runClient      # test in-game
```

Versions live in `gradle.properties` and **must be checked against
<https://fabricmc.net/develop> first** — the values there are placeholders that
could not be verified offline.

### Verifying the core without Minecraft

```bash
./tools/verify-core.sh
```

Compiles `core/` and the test suite against gson, slf4j and JUnit from Maven
Central and runs them on a plain JVM. It fetches those four jars on first run.
`./gradlew test` runs the same sources once the toolchain is reachable.

### Textures

`tools/GenerateTextures.java` draws every texture in the mod:

```bash
java tools/GenerateTextures.java
```

The art is code so it can be reviewed and adjusted in a diff rather than
re-exported from an image editor.

---

## Architecture

```
core/                       no Minecraft imports anywhere below this line
├── data/                   Folder, FolderConfig, FolderRepository, FolderType
├── storage/                JsonStorage, FolderCodec, FoldersSettings
├── identity/               ItemIdentity — the id rules
├── drag/                   DragManager, DragState, DragPayload, DropTarget
├── animation/              Animation, Easing
└── view/                   FolderViewModel, DisplayRow — the accordion layout

identity/                   resolvers: LevelSummary / ServerInfo / Pack -> id
ui/                         GuiCompat, FolderRowRenderer, GhostRenderer,
                            FolderContextMenu, IconManager, FolderIconPicker
integration/                FolderListController + one subclass per screen
mixin/                      thin injection points only
```

The rule the layering enforces is the one from §44 of the brief: a mixin
connects, a controller decides, a repository holds, storage persists. No mixin
contains folder logic and no folder logic imports a mixin.

Three decisions are worth calling out.

**`core` has no Minecraft imports.** Not a stylistic preference — it is what
makes the interesting parts testable. The invariants that actually break a mod
(an item ending up in two folders, a config that eats itself, an animation tied
to frame rate) are all exercised by `tools/verify-core.sh` in under half a
second, with no game and no mappings.

**Every version-sensitive call is funnelled through `ui/GuiCompat`.**
`DrawContext` is the fastest-moving API in the client and 26.2 moves it again.
Eight methods there carry the whole mod's rendering, so a mappings bump is a
one-file fix rather than a hunt through the renderer, the entries and the menu.
Files that need checking against real mappings are all marked `MAPPING NOTE`.

**Folder rows are `FolderEntryDelegate` plus a five-line shell per screen.** The
three shells subclass whatever entry type their list demands; everything they do
lives in the shared delegate. That is §45 — nothing is designed around one
particular `EntryListWidget.Entry`.

---

## How the pieces work

### Identity (§6)

Never a list index, never a display name.

| Type | Key | Survives |
|---|---|---|
| World | save directory name | renaming the world in-game |
| Server | `host:port`, lowercased, default port implied | renaming the server |
| Resource pack | profile id (`file/shaders.zip`) | two packs sharing a title |

Minecraft exposes no world UUID to the client, and writing a marker file into
`saves/` would be exactly the interference §77 forbids, so the directory name is
the key. It is what Minecraft itself uses, and it does not change when the player
renames a world — the case §6 actually cares about.

### Ordering (§60, §61)

`config/folders/*.json` stores a `root` array: the full top-level order, where a
folder is `folder:<uuid>` and anything else is an item id. A folder can therefore
sit between two worlds and stay there.

Items Minecraft has that the model has not seen are inserted **next to their
vanilla neighbour**, not appended. A brand new world, which vanilla sorts to the
top of a last-played list, lands at the top; a brand new server, which
`servers.dat` appends, lands at the bottom. One rule, no per-screen special
casing.

> **Trade-off.** Because the stored order wins, worlds stop re-sorting by
> last-played once Folders has seen them. That is what §60 asks for and what §62
> implies by listing "by date" as a *future* sort mode, but it is a visible
> behaviour change. If it turns out to be the wrong call, the fix is local:
> anchor folders to a neighbouring item instead of storing loose items in `root`.

### Corruption (§8)

An unreadable `worlds.json` is moved to `worlds.json.bak`, logged, and replaced
with an empty config; the game keeps going. A second corruption gets a
timestamped name rather than overwriting the first backup. A file that is merely
*partly* broken loses only the broken entries — a bad UUID, a duplicate, an id of
the wrong type, an icon path with `..` in it are each dropped individually. A
config written by a newer Folders is quarantined rather than mangled.

### Saving (§50, §51)

Nothing writes during rendering. Mutations set a dirty flag; the operation that
finishes (drop, rename, delete, toggle) calls `saveIfDirty`. Serialisation is
synchronous — cheap, and it snapshots the state before the player can change it
again — and the write itself goes to a background thread, one chain per file, so
two saves cannot interleave or land out of order. Writes are atomic via
tmp-then-move.

### The accordion (§26–§29)

One number per folder: how far open it is. Row positions fall out of it, so no
entry needs its own timer. `EntryListWidgetMixin` swaps vanilla's
`index * itemHeight` for a lookup, but only on lists Folders has installed a
layout on — every other list in the game takes an early return and is untouched.

Animations run on elapsed wall time, 200 ms, ease-out-cubic. Retargeting
mid-flight eases from the current value, so opening and immediately closing a
folder does not snap.

### Drag versus click (§16)

One `DragManager` for all three screens. A press is not a drag until the pointer
travels 5 px, so a click on a world still opens the world and a click on a folder
still opens the folder.

**Resource packs are the interesting case (§49)**, because vanilla already uses
drag there to move packs between the two lists. The two modes are split by area,
not by heuristics: a drag starting on the pack's 32 px icon is a Folders drag,
a drag starting anywhere else on the row is the vanilla one.

### Resource pack folders (§48)

The pack screen is two lists and a pack is physically in one of them. A folder is
remembered per pack id, so it appears in whichever list its packs are in — the
same folder can show on both sides, each showing only the packs on that side.
Enable/disable always goes through `ResourcePackOrganizer`; Minecraft stays the
source of truth for what is on, and the folder stores no copy of it (§34).

### Icons (§22, §52, §75)

The file on disk is named after the folder UUID, so a folder called
`../../server` cannot escape the icon directory — the user's name never touches a
path. The source PNG is copied in, so deleting the original does not break
anything. Files over 2 MB or 1024 px, non-PNGs and undecodable images all fall
back to the default icon, and a failure is remembered so a broken icon is not
retried every frame.

---

## Mapping checklist

Everything below is written against 1.21.x Yarn shapes and needs confirming.
Grep for `MAPPING NOTE`; these are the load-bearing ones.

| File | What to check | If wrong |
|---|---|---|
| `ui/GuiCompat` | `DrawContext` texture / scissor / matrix signatures | Nothing draws |
| `mixin/EntryListWidgetMixin` | `getRowTop`, `getMaxPosition`, `getEntryAtPosition`, and vanilla's row-top formula | Rows misplaced, clicks land on the wrong row |
| `mixin/world/WorldEntryAccessor` | field `level`; `LevelSummary.getName()` is the **directory**, `getDisplayName()` the title | Folders keyed on display name — the thing §77.9 forbids |
| `mixin/server/ServerEntryAccessor` | field `server`; `ServerInfo.address`, `.online` | No server identity, no online dot |
| `mixin/pack/ResourcePackEntryAccessor` | field `pack`; `Pack.getName()` is the **profile id** | Packs keyed on display name |
| `mixin/server/MultiplayerScreenAccessor` | field `serverListPinger`, and `add(...)`'s arity | "Refresh ping" does nothing |
| `integration/pack/FolderPackStub` | `ResourcePackOrganizer.Pack` still an interface | Folder rows cannot exist in the pack list |
| screen mixins | `levelList`, `serverListWidget`, `availablePackList`; `show`, `setServers` | No button, or the list never rebuilds |
| entry classes | `Entry#render` signature | Folder rows do not draw |

`FolderPackStub` deserves a second look. `PackListWidget` is an
`EntryListWidget<ResourcePackEntry>`, so every row must be a pack entry and every
pack entry must hold a pack. Rather than pass a null and hope nothing
dereferences it, a folder row carries a stub whose accessors all answer something
harmless and whose mutators do nothing — if a vanilla path ever does reach a
folder row, it changes no pack state.

---

## Not implemented

Deliberate omissions, all P3 in the brief: search (§64), extra sort modes (§62),
nested folders (§10 rules them out for v1), recently-used indicators (§63),
shift-drag quick move (§65). The settings from §70 are stored and honoured but
have no options screen yet — edit `config/folders/settings.json`.

Root-level reordering of loose *items* by drag is not wired up either; dragging
between folders and the root is, and ordering *inside* a folder is. §19 marks
this as deferrable, and the data model already supports it.

## Layout on disk

```
.minecraft/config/folders/
├── worlds.json
├── servers.json
├── resource_packs.json
├── settings.json
└── icons/
    └── <folder-uuid>.png
```

Removing the mod leaves this directory in place, so reinstalling restores every
folder (§57).

## Licence

MIT.
