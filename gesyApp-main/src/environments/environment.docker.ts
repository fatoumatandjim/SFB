/**
 * Environnement pour le build Docker.
 * L'API_URL est injectée au moment du build via ARG dans le Dockerfile.
 */
export const environment = {
  production: true,
  apiUrl: '__API_URL__',
};
