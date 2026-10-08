package com.jttw.engine.progression.skilltree;

import com.jttw.engine.character.byakko.ByakkoSkillTree;
import com.jttw.engine.character.dragon.DragonSkillTree;
import com.jttw.engine.character.erlang.ErlangShenSkillTree;
import com.jttw.engine.character.marshal.MarshalSkillTree;
import com.jttw.engine.character.nezha.NezhaSkillTree;
import com.jttw.engine.character.peng.PengSkillTree;
import com.jttw.engine.character.phoenix.PhoenixSkillTree;
import com.jttw.engine.character.turtle.TurtleSkillTree;
import com.jttw.engine.character.wukong.WukongSkillTree;
import com.jttw.engine.character.yuandi.YuanDiSkillTree;

import javax.annotation.Nullable;
import java.util.List;

/** Every character's skill tree, and lookup of the tree a node id belongs to. */
public final class SkillTrees {

    private SkillTrees() {}

    public static List<SkillTree> all() {
        return List.of(
                WukongSkillTree.TREE, ErlangShenSkillTree.TREE, DragonSkillTree.TREE,
                TurtleSkillTree.TREE, ByakkoSkillTree.TREE, PhoenixSkillTree.TREE,
                YuanDiSkillTree.TREE, PengSkillTree.TREE, NezhaSkillTree.TREE, MarshalSkillTree.TREE,
                com.jttw.engine.character.ninespirits.NineSpiritsSkillTree.TREE);
    }

    /** The tree whose prefix names this node, or null if no tree claims it. */
    @Nullable
    public static SkillTree treeOf(String nodeId) {
        for (SkillTree tree : all()) {
            if (tree.owns(nodeId)) return tree;
        }
        return null;
    }

    /** The node with this id in whichever tree owns it, or null. */
    @Nullable
    public static SkillNode resolveNode(String nodeId) {
        SkillTree tree = treeOf(nodeId);
        return tree == null ? null : tree.get(nodeId);
    }
}
