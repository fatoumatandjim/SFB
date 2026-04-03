import { Client } from '../services/clients.service';

/** Couleurs d’avatar alignées sur l’UI contacts */
const CONTACT_AVATAR_COLORS = [
  'blue',
  'green',
  'purple',
  'orange',
  'red',
  'teal',
  'pink',
  'indigo'
] as const;

export type TypeClient = Client['type'];

export const DEFAULT_TYPE_CLIENT: TypeClient = 'ENTREPRISE';
export type TypeFournisseurApi = 'ACHAT' | 'TRANSPORT';
export const DEFAULT_TYPE_FOURNISSEUR: TypeFournisseurApi = 'ACHAT';

/** Options des listes déroulantes (valeur API + libellé UI) — source unique DRY */
export const CLIENT_TYPE_OPTIONS: ReadonlyArray<{ value: TypeClient; label: string }> = [
  { value: 'ENTREPRISE', label: 'Entreprise' },
  { value: 'PARTICULIER', label: 'Particulier' },
  { value: 'GOUVERNEMENT', label: 'Gouvernement' }
];

export const FOURNISSEUR_TYPE_OPTIONS: ReadonlyArray<{ value: TypeFournisseurApi; label: string }> = [
  { value: 'ACHAT', label: "Fournisseur d'achat" },
  { value: 'TRANSPORT', label: 'Fournisseur transport' }
];

export function buildAvatarFromNom(nom: string): { initiales: string; couleur: string } {
  const words = nom.split(/\s+/).filter(Boolean);
  const initiales =
    words.length >= 2
      ? (words[0][0] + words[1][0]).toUpperCase()
      : nom.substring(0, 2).toUpperCase();
  const couleur = CONTACT_AVATAR_COLORS[Math.abs(nom.charCodeAt(0)) % CONTACT_AVATAR_COLORS.length];
  return { initiales, couleur };
}

/** Normalise la valeur API / formulaire vers ACHAT | TRANSPORT */
export function normalizeTypeFournisseur(raw?: string | null): TypeFournisseurApi {
  return raw === 'TRANSPORT' ? 'TRANSPORT' : 'ACHAT';
}

export function libelleTypeFournisseur(type?: string | null): string {
  const t = normalizeTypeFournisseur(type);
  const opt = FOURNISSEUR_TYPE_OPTIONS.find((o) => o.value === t);
  return opt?.label ?? FOURNISSEUR_TYPE_OPTIONS[0].label;
}

export function isValidTypeClient(value: unknown): value is TypeClient {
  return value === 'PARTICULIER' || value === 'ENTREPRISE' || value === 'GOUVERNEMENT';
}

export function isValidTypeFournisseur(value: unknown): value is TypeFournisseurApi {
  return value === 'ACHAT' || value === 'TRANSPORT';
}

export interface NewClientFormState {
  nom: string;
  email: string;
  telephone: string;
  adresse: string;
  type: TypeClient;
  ville: string;
  pays: string;
}

export function createEmptyNewClient(): NewClientFormState {
  return {
    nom: '',
    email: '',
    telephone: '',
    adresse: '',
    type: DEFAULT_TYPE_CLIENT,
    ville: '',
    pays: ''
  };
}

export interface NewFournisseurFormState {
  nom: string;
  email: string;
  telephone: string;
  adresse: string;
  ville: string;
  pays: string;
  contactPersonne: string;
  typeFournisseur: TypeFournisseurApi;
}

export function createEmptyNewFournisseur(): NewFournisseurFormState {
  return {
    nom: '',
    email: '',
    telephone: '',
    adresse: '',
    ville: '',
    pays: '',
    contactPersonne: '',
    typeFournisseur: DEFAULT_TYPE_FOURNISSEUR
  };
}
