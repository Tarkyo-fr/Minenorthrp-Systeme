# MineNorth Systeme V77 — Vente, location et revente des maisons

## Vente permanente
- Les maisons sont mises sur le marché par les OP.
- Lorsqu'un joueur achète une maison, toutes les portes personnelles portant le même nom de maison lui sont transférées.
- Le prix payé est mémorisé comme prix d'achat initial de la maison.
- Le propriétaire peut ensuite revendre la maison.
- Lors d'une revente, il récupère 50 % du prix d'achat initial via la banque.
- La maison repasse immédiatement en vente au prix d'achat initial.
- Toutes les portes personnelles du groupe sont remises en vente.

## Location temporaire
- Le prix et la durée de location sont conservés.
- À expiration, la location est automatiquement remise disponible au même prix et pour la même durée.
- Les annonces sont propagées à toutes les portes de la même maison.

## Compatibilité
- Les anciennes portes sans `PurchasePrice` sont chargées avec un prix d'achat de 0.
- Le système bancaire fourni par MineNorth EuroBank est utilisé via son API publique.
- Le mod bancaire d'origine n'est pas modifié.
