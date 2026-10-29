package me.mss1r.recruitsrtscommand.client.gui.worldmap.command;

record TacticalSymbolPalette(int face, int edge, int ink) {
    static final int OWN_COLOR = 0xFF43BF6A;
    static final int ALLY_COLOR = 0xFF4E8FE6;
    static final int NEUTRAL_COLOR = 0xFFD6B76A;
    static final int ENEMY_COLOR = 0xFFE46058;

    static TacticalSymbolPalette forRelation(int relation) {
        return new TacticalSymbolPalette(relation, 0xFF101722, 0xFF14202D);
    }
}
