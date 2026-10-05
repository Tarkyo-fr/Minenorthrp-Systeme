# MineNorth Systeme V71

## Interfaces

Les interfaces MineNorth Systeme ont été harmonisées avec la charte visuelle observée dans les mods de référence fournis :
- panneau clair et bandeau bleu nuit ;
- cartes avec accent latéral ;
- boutons avec états normal/survol/inactif ;
- listes, cartes et défilement ;
- hiérarchie visuelle et affichage compact.

Les mods fournis par l'ami ne sont pas modifiés. Le logo et les principes graphiques sont adaptés/copied dans MineNorth Systeme.

Le système de banque fourni est utilisé comme dépendance pour les paiements de portes, mais son code/interface n'est pas modifié.

## Portes : vente et location

Une porte personnelle peut être proposée depuis son panneau de gestion :
- Vente définitive : prix en euros, paiement par carte bancaire EuroBank, transfert de propriété.
- Location temporaire : prix + nombre de jours, paiement par carte bancaire EuroBank, accès temporaire au locataire.
- Les locations expirent automatiquement.
- Les portes Police, Pompiers, Entreprise et Organisation ne sont pas vendables/louables.
- Les annonces sont persistées dans `config/Minenorth-systeme/porte.toml`.

La gestion se fait avec sneak + clic droit sur la porte.
