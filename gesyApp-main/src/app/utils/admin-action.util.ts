import { AuthService } from '../services/auth.service';
import { ToastService } from '../nativeComp/toast/toast.service';

export const MSG_ADMIN_ACTION_FORBIDDEN = 'Cette action est réservée aux administrateurs.';

/**
 * Vérifie le rôle admin ; sinon affiche un toast et retourne false.
 */
export function guardAdmin(
  auth: AuthService,
  toast: ToastService,
  message: string = MSG_ADMIN_ACTION_FORBIDDEN
): boolean {
  if (auth.isAdmin()) {
    return true;
  }
  toast.error(message);
  return false;
}
