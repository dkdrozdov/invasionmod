package com.invasionmod.access;

import net.minecraft.util.Identifier;

public interface ServerPlayerEntityAccess {
    boolean invasionmod$getNeedReturnLoot();

    void invasionmod$setNeedReturnLoot(boolean returnLoot);

    Identifier invasionmod$getReturnLootWorld();

    void invasionmod$setReturnLootWorld(Identifier _needReturnLoot);

    boolean invasionmod$getShouldGetStone();
    void invasionmod$setShouldGetStone(boolean _shouldGetStone);
}
