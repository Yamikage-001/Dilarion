import { isTauri } from './platform';

// A newer signed desktop build is waiting. The app does not install it until
// the user taps Update on the static screen.
export interface AvailableUpdate {
  version: string;
  install: () => Promise<void>;
}

let ranOnce = false;

export async function checkForUpdate(): Promise<AvailableUpdate | null> {
  if (ranOnce || !isTauri()) return null;
  ranOnce = true;
  try {
    const { check } = await import('@tauri-apps/plugin-updater');
    const update = await check();
    if (!update) return null;
    return {
      version: update.version,
      install: async () => {
        await update.downloadAndInstall();
        const { relaunch } = await import('@tauri-apps/plugin-process');
        await relaunch();
      },
    };
  } catch {
    // No endpoints (test build), offline, or the check itself failed.
    // Leave the current version running. There is nothing to show.
    return null;
  }
}
