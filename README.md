# Folders
A client-side Fabric mod that adds folders to the singleplayer, multiplayer and
resource pack screens.

Folders are a view, not a storage format. `saves/`, `servers.dat` and
`resourcepacks/` are never read for anything but identity and never written at
all; grouping lives in `config/folders/*.json`. Uninstall the mod and Minecraft
behaves exactly as it did before, with every world, server and pack where it was.

---

## Status

**The core is implemented and tested. Worlds and servers are written against
real 26.2 signatures. Resource packs are not in the build.**

That split is what the environment allowed: `maven.fabricmc.net` and
`libraries.minecraft.net` are blocked by the network policy where this was
developed, so Gradle cannot resolve Loom or Minecraft here and the 26.2 mappings
could not be consulted directly. The class and method names below were taken
from ChatUtils, which targets the same version — so the shapes are real, but the
parts ChatUtils does not itself use (the selection lists above all) are inferred.
Rather than guess quietly, the mod is arranged so the guessing is confined to as
few lines as possible.

| Layer | State |
|---|---|
| `core/` — model, storage, identity, drag, animation, layout | Compiled and unit tested, 63 tests green |
| Worlds, servers — rows, drag, rename, context menu | Written against real 26.2 signatures |
| Resource packs | **Removed from the build**, see below |
| Server ping / online dot | **Disabled**, see below |
| Textures, translations, manifests | Present |

### What is switched off, and why

- **The expand animation.** 26.2's `AbstractSelectionList` positions rows itself
  (`getNextY`, `repositionEntries`) and has no `getMaxPosition` to override, so
  the accordion has to drive each entry's own height instead of the list's
  arithmetic. Until then folders open and close instantly. The animation code in
  `core` is unchanged and tested; only the integration bypasses it.
- **Drag and drop.** Neither selection list overrides `mouseDragged` or
  `mouseReleased`, so there is nowhere on them to inject; the hook belongs on
  `AbstractWidget` or the screen, and neither has been checked yet. In the
  meantime items go into folders by **right-clicking a world or server**, which
  uses only confirmed API and works from the keyboard too. Dragging is still the
  nicer gesture and is still the plan.
- **Resource pack folders.** Now unblocked by `dumpApi`:
  `TransferableSelectionList.PackEntry` is an inner class constructed as
  `list.new PackEntry(minecraft, list, pack)`, `PackSelectionModel.Entry` is a
  known interface, and `updateList` is the rebuild hook. Not yet rewritten.
- **"Refresh ping".** `ServerStatusPinger.pingServer` takes an
  `EventLoopGroupHolder` this mod has no clean way to obtain.

The online dot and count are back: 26.2 dropped `ServerData.online` but kept
`ping`, which is what the dot actually means.

---

## Building

```bash
./gradlew build          # jar in build/libs/
./gradlew runClient      # test in-game
```

Use the wrapper, not the IDE's bundled Gradle. It is pinned to 9.6.1 because
Loom `1.17-SNAPSHOT` is a moving target: recent snapshots declare a plugin API
version that Gradle 9.0 rejects outright, and the failure reads as an
unresolvable artifact rather than a version mismatch. In IntelliJ, set
*Build tools → Gradle → Use Gradle from: gradle-wrapper.properties*.

Versions in `gradle.properties` match ChatUtils: Minecraft `26.2`, loader
`0.19.3`, Fabric API `0.155.2+26.2`, Loom `1.17-SNAPSHOT`, Java 25, Mojang
mappings (no `mappings` dependency — Loom defaults to them).

The Gradle JVM should be 21 or newer; the Java 25 toolchain is fetched by
Gradle if the selected JDK is older.

### Verifying the core without Minecraft

```bash
./tools/verify-core.sh
```

Compiles `core/` and the test suite against gson, slf4j and JUnit from Maven
Central and runs them on a plain JVM. It fetches those four jars on first run.
`./gradlew test` runs the same sources once the toolchain is reachable.

### Finding vanilla signatures

```bash
./gradlew dumpApi        # -> build/api.txt
```

Dumps the signatures of every vanilla class Folders hooks into, using the exact
remapped jar Gradle already resolved. Worth running after any version bump: the
selection lists and their entry classes are where this mod is fragile, and
reading the real signatures is faster than discovering them one crash at a time.

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
src/main/java/dev/miklires/folders/
├── Folders                 mod-wide handles
└── core/                   no Minecraft imports anywhere below this line
    ├── data/               Folder, FolderConfig, FolderRepository, FolderType
    ├── storage/            JsonStorage, FolderCodec
    ├── identity/           ItemIdentity — the id rules
    ├── drag/               DragManager, DragState, DragPayload, DropTarget
    ├── animation/          Animation, Easing
    └── view/               FolderViewModel, DisplayRow — the accordion layout

src/client/java/dev/miklires/folders/
├── client/config/          FoldersConfig (YACL), ConfigScreen
├── client/identity/        LevelSummary / ServerData / PackSelectionModel -> id
├── client/ui/              GuiCompat, FolderRowRenderer, GhostRenderer,
│                           FolderContextMenu, IconManager, FolderIconPicker
├── client/integration/     FolderListController + one subclass per screen
└── mixin/client/           thin injection points only
```

The layering enforces one rule: a mixin connects, a controller decides, a repository holds, storage persists. No mixin
contains folder logic and no folder logic imports a mixin.

Three decisions are worth calling out.

**`core` has no Minecraft imports.** Not a stylistic preference — it is what
makes the interesting parts testable. The invariants that actually break a mod
(an item ending up in two folders, a config that eats itself, an animation tied
to frame rate) are all exercised by `tools/verify-core.sh` in under half a
second, with no game and no mappings.

**Every version-sensitive call is funnelled through `client/ui/GuiCompat`.**
26.2 draws through `GuiGraphicsExtractor` — widgets extract a render state
instead of issuing draw calls — and the signatures moved with it. A dozen methods
there carry the whole mod's rendering, so the next time that happens it is a
one-file fix rather than a hunt through the renderer, the entries and the menu.
Files needing a check against real mappings are all marked `MAPPING NOTE`.

**Folder rows are `FolderEntryDelegate` plus a five-line shell per screen.** The
three shells subclass whatever entry type their list demands; everything they do
lives in the shared delegate. Nothing is designed around one particular selection-list entry class, which is
the part most likely to be renamed out from under the mod.

---

## How the pieces work

### Identity

Never a list index, never a display name.

| Type | Key | Survives |
|---|---|---|
| World | save directory name | renaming the world in-game |
| Server | `host:port`, lowercased, default port implied | renaming the server |
| Resource pack | profile id (`file/shaders.zip`) | two packs sharing a title |

Minecraft exposes no world UUID to the client, and writing a marker file into
`saves/` is exactly the interference this mod exists to avoid, so the directory
name is the key. It is what Minecraft itself uses, and it does not change when the player
renames a world, which is the case that matters.

### Ordering

`config/folders/*.json` stores a `root` array: the full top-level order, where a
folder is `folder:<uuid>` and anything else is an item id. A folder can therefore
sit between two worlds and stay there.

Items Minecraft has that the model has not seen are inserted **next to their
vanilla neighbour**, not appended. A brand new world, which vanilla sorts to the
top of a last-played list, lands at the top; a brand new server, which
`servers.dat` appends, lands at the bottom. One rule, no per-screen special
casing.

> **Trade-off.** Because the stored order wins, worlds stop re-sorting by
> last-played once Folders has seen them. That is what a manual ordering means,
> but it is a visible behaviour change. If it turns out to be the wrong call, the fix is local:
> anchor folders to a neighbouring item instead of storing loose items in `root`.

### Corruption

An unreadable `worlds.json` is moved to `worlds.json.bak`, logged, and replaced
with an empty config; the game keeps going. A second corruption gets a
timestamped name rather than overwriting the first backup. A file that is merely
*partly* broken loses only the broken entries — a bad UUID, a duplicate, an id of
the wrong type, an icon path with `..` in it are each dropped individually. A
config written by a newer Folders is quarantined rather than mangled.

### Saving

Nothing writes during rendering. Mutations set a dirty flag; the operation that
finishes (drop, rename, delete, toggle) calls `saveIfDirty`. Serialisation is
synchronous — cheap, and it snapshots the state before the player can change it
again — and the write itself goes to a background thread, one chain per file, so
two saves cannot interleave or land out of order. Writes are atomic via
tmp-then-move.

### The accordion

One number per folder: how far open it is. Row positions fall out of it, so no
entry needs its own timer. `AbstractSelectionListMixin` swaps vanilla's
`index * itemHeight` for a lookup, but only on lists Folders has installed a
layout on — every other list in the game takes an early return and is untouched.

Animations run on elapsed wall time, 200 ms, ease-out-cubic. Retargeting
mid-flight eases from the current value, so opening and immediately closing a
folder does not snap.

### Drag versus click

One `DragManager` for all three screens. A press is not a drag until the pointer
travels 5 px, so a click on a world still opens the world and a click on a folder
still opens the folder.

**Resource packs are the interesting case **, because vanilla already uses
drag there to move packs between the two lists. The two modes are split by area,
not by heuristics: a drag starting on the pack's 32 px icon is a Folders drag,
a drag starting anywhere else on the row is the vanilla one.

### Resource pack folders

The pack screen is two lists and a pack is physically in one of them. A folder is
remembered per pack id, so it appears in whichever list its packs are in — the
same folder can show on both sides, each showing only the packs on that side.
Enable/disable always goes through `ResourcePackOrganizer`; Minecraft stays the
source of truth for what is on, and the folder stores no copy of it.

### Icons

The file on disk is named after the folder UUID, so a folder called
`../../server` cannot escape the icon directory — the user's name never touches a
path. The source PNG is copied in, so deleting the original does not break
anything. Files over 2 MB or 1024 px, non-PNGs and undecodable images all fall
back to the default icon, and a failure is remembered so a broken icon is not
retried every frame.

---

## Mapping checklist

What is left unverified, all of it flagged with `MAPPING NOTE`:

| File | What to check |
|---|---|
| `integration/FolderEntryDelegate` | `KeyEvent.key()` and `CharacterEvent.codepoint()` — read in one method each, so a wrong guess is a two-line fix |
| `mixin/client/AbstractSelectionListMixin` | `getRowTop` / `getMaxPosition`, and that vanilla's row top really is `contentTop + index * itemHeight` |
| `ui/GuiCompat` | `enableScissor` / `disableScissor` on the extractor |

Two things are known-wrong rather than unverified. `AbstractSelectionList.Entry`
is protected, so the hit-testing injection had to go: during the ~200 ms a folder
spends animating, a click is tested against vanilla's uniform row arithmetic and
can land on the neighbouring row. And 26.2's pose stack is a `Matrix3x2fStack`,
purely 2D, so there is no z-layer — the ghost preview and the context menu are on
top only because they are drawn last.

---

## Not implemented

Deliberate omissions: search, extra sort modes,
nested folders, recently-used indicators, shift-drag quick move. Settings live
behind the Mod Menu cog (YACL, `config/folders.json`).

Root-level reordering of loose *items* by drag is not wired up either; dragging
between folders and the root is, and ordering *inside* a folder is. The data
model already supports it.

## Layout on disk

```
.minecraft/config/folders/
├── worlds.json
├── servers.json
├── resource_packs.json
└── icons/
    └── <folder-uuid>.png

.minecraft/config/folders.json     # the YACL settings
```

Removing the mod leaves this directory in place, so reinstalling restores every
folder.

## Licence

All rights reserved.
