package com.attacktimer;

/*
 * Copyright (c) 2021, Matsyir <https://github.com/matsyir>
 * Copyright (c) 2020, Mazhar <https://twitter.com/maz_rs>
 * Copyright (c) 2024-2026, Lexer747 <https://github.com/Lexer747>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import net.runelite.api.gameval.AnimationID;
import org.apache.commons.lang3.StringUtils;

public enum AnimationData
{
    // MELEE
    MELEE_VIGGORAS_CHAINMACE(AnimationID.WILD_CAVE_CHAINMACE_CRUSH, CombatSkill.MELEE),
    MELEE_DAGGER_SLASH(AnimationID.HUMAN_DDAGGER_LUNGE, CombatSkill.MELEE), // tested w/ dds
    MELEE_SPEAR_STAB(AnimationID.HUMAN_DSPEAR_STAB, CombatSkill.MELEE), // tested w/ zammy hasta
    MELEE_SWORD_STAB(AnimationID.HUMAN_SWORD_STAB, CombatSkill.MELEE), // tested w/ dragon sword, obby sword, d long
    MELEE_SCIM_SLASH(AnimationID.HUMAN_SWORD_SLASH, CombatSkill.MELEE), // tested w/ rune & dragon scim, d sword, VLS, obby sword

    MELEE_LANCE_STAB(AnimationID.HUMAN_DHUNTER_LANCE_ATTACK, CombatSkill.MELEE),
    MELEE_LANCE_CRUSH(AnimationID.HUMAN_DHUNTER_LANCE_CRUSH, CombatSkill.MELEE),
    MELEE_LANCE_SLASH(AnimationID.HUMAN_DHUNTER_LANCE_SLASH, CombatSkill.MELEE),

    MELEE_FANG_STAB(AnimationID.HUMAN_OSMUMTENS_FANG, CombatSkill.MELEE), // tested w/ fang
    MELEE_FANG_SPEC(AnimationID.OLAF2_BRINE_SABRE_SPECIAL, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // tested w/ fang spec

    MELEE_GENERIC_SLASH(AnimationID.HUMAN_AXE_CHOP, CombatSkill.MELEE), // tested w/ zuriel's staff, d long slash, dclaws regular slash

    MELEE_BATTLEAXE_SLASH(AnimationID.HUMAN_AXE_HACK, CombatSkill.MELEE), // tested w/ rune baxe
    MELEE_MACE_STAB(AnimationID.HUMAN_BLUNT_SPIKE, CombatSkill.MELEE), // tested w/ d mace
    MELEE_BATTLEAXE_CRUSH(AnimationID.HUMAN_BLUNT_POUND, CombatSkill.MELEE), // tested w/ rune baxe, dwh & statius warhammer animation, d mace
    MELEE_2H_CRUSH(AnimationID.HUMAN_DHSWORD_CHOP, CombatSkill.MELEE), // tested w/ rune & dragon 2h
    MELEE_2H_SLASH(AnimationID.HUMAN_DHSWORD_SLASH, CombatSkill.MELEE), // tested w/ rune & dragon 2h
    MELEE_STAFF_CRUSH_2(AnimationID.HUMAN_STAFF_PUMMEL, CombatSkill.MELEE), // tested w/ ancient staff, 3rd age wand
    MELEE_STAFF_CRUSH_3(AnimationID.HUMAN_STAFFORB_PUMMEL, CombatSkill.MELEE), // Common staff crush. Air/fire/etc staves, smoke battlestaff, SOTD/SOL crush, zammy hasta crush
    MELEE_PUNCH(AnimationID.HUMAN_UNARMEDPUNCH, CombatSkill.MELEE),
    MELEE_KICK(AnimationID.HUMAN_UNARMEDKICK, CombatSkill.MELEE),
    MELEE_STAFF_STAB(AnimationID.HUMAN_SPEAR_SPIKE, CombatSkill.MELEE), // tested w/ SOTD/SOL jab, vesta's spear stab, c hally
    MELEE_SPEAR_CRUSH(AnimationID.HUMAN_SPEAR_LUNGE, CombatSkill.MELEE), // tested w/ vesta's spear
    MELEE_STAFF_SLASH(AnimationID.HUMAN_SCYTHE_SWEEP, CombatSkill.MELEE), // tested w/ SOTD/SOL slash, zammy hasta slash, vesta's spear slash, c hally
    MELEE_DLONG_SPEC(AnimationID.CLEAVE, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // tested w/ d long spec, also thammaron's sceptre crush (????)...
    MELEE_DRAGON_MACE_SPEC(AnimationID.SHATTER, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d),
    MELEE_DRAGON_DAGGER_SPEC(AnimationID.PUNCTURE, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d),
    MELEE_DRAGON_WARHAMMER_SPEC(AnimationID.DRAGON_WARHAMMER_SA_PLAYER, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // tested w/ dwh, statius warhammer spec
    MELEE_ABYSSAL_WHIP(AnimationID.SLAYER_ABYSSAL_WHIP_ATTACK, CombatSkill.MELEE), // tested w/ whip, tent whip
    MELEE_GRANITE_MAUL(AnimationID.SLAYER_GRANITE_MAUL_ATTACK, CombatSkill.MELEE), // tested w/ normal gmaul, ornate maul
    MELEE_GRANITE_MAUL_SPEC(AnimationID.SLAYER_GRANITE_MAUL_SPECIAL_ATTACK, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // tested w/ normal gmaul, ornate maul
    MELEE_DHAROKS_GREATAXE_CRUSH(AnimationID.BARROW_DHAROK_SLASH, CombatSkill.MELEE),
    MELEE_DHAROKS_GREATAXE_SLASH(AnimationID.BARROW_DHAROK_CRUSH, CombatSkill.MELEE),
    MELEE_AHRIMS_STAFF_CRUSH(AnimationID.BARROWS_QUARTERSTAFF_ATTACK, CombatSkill.MELEE),
    MELEE_OBBY_MAUL_CRUSH(AnimationID.DRAGON_PICKAXE_ANIM, CombatSkill.MELEE),
    MELEE_ABYSSAL_DAGGER_STAB(AnimationID.ABYSSAL_DAGGER_LUNGE, CombatSkill.MELEE), // spec un-tested
    MELEE_ABYSSAL_BLUDGEON_CRUSH(AnimationID.ABYSSAL_BLUDGEON_CRUSH, CombatSkill.MELEE),
    MELEE_ABYSSAL_BLUDGEON_SPEC(AnimationID.ABYSSAL_BLUDGEON_SPECIAL_ATTACK, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d),
    MELEE_LEAF_BLADED_BATTLEAXE_CRUSH(AnimationID.BATTLEAXE_CRUSH, CombatSkill.MELEE),
    MELEE_INQUISITORS_MACE(AnimationID.HUMAN_INQUISITORS_MACE_CRUSH, CombatSkill.MELEE),
    MELEE_BARRELCHEST_ANCHOR_CRUSH(AnimationID.BRAIN_PLAYER_ANCHOR_ATTACK, CombatSkill.MELEE),
    MELEE_BARRELCHEST_ANCHOR_CRUSH_SPEC(AnimationID.BRAIN_PLAYER_ANCHOR_SPECIAL_ATTACK, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d),
    MELEE_LEAF_BLADED_BATTLEAXE_SLASH(AnimationID.GODWARS_GODSWORD_ZAMORAK_PLAYER, CombatSkill.MELEE),
    MELEE_GODSWORD_SLASH(AnimationID.DH_SWORD_UPDATE_SLASH, CombatSkill.MELEE), // tested w/ AGS, BGS, ZGS, SGS, AGS(or) sara sword
    MELEE_GODSWORD_CRUSH(AnimationID.DH_SWORD_UPDATE_SMASH, CombatSkill.MELEE), // tested w/ AGS, BGS, ZGS, SGS, sara sword
    MELEE_GODSWORD_DEFENSIVE(AnimationID.DH_SWORD_UPDATE_BLOCK, CombatSkill.MELEE), // tested w/ BGS
    MELEE_RUNE_CLAWS_SPEC(AnimationID.IMPALE, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d),
    MELEE_DRAGON_CLAWS_SPEC(AnimationID.HUMAN_DRAGON_CLAWS_SPEC, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d),
    MELEE_VLS_SPEC(AnimationID.HUMAN_DRAGON_SWORD_SPEC, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // both VLS and dragon sword spec
    MELEE_ELDER_MAUL(AnimationID.HUMAN_ELDER_MAUL_ATTACK, CombatSkill.MELEE),
    MELEE_ZAMORAK_GODSWORD_SPEC(AnimationID.ZGS_SPECIAL_PLAYER, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // tested zgs spec
    MELEE_ELDER_MAUL_SPEC(AnimationID.HUMAN_ELDER_MAUL_SPEC, CombatSkill.MELEE),
    MELEE_ZAMORAK_GODSWORD_OR_SPEC(AnimationID.ZGS_SPECIAL_ORNATE_PLAYER, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // verified 22/06/2024, assumed due to ags(or)
    MELEE_SARADOMIN_GODSWORD_SPEC(AnimationID.SGS_SPECIAL_PLAYER, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // tested sgs spec
    MELEE_SARADOMIN_GODSWORD_OR_SPEC(AnimationID.SGS_SPECIAL_ORNATE_PLAYER, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // verified 22/06/2024, assumed due to ags(or)
    MELEE_BANDOS_GODSWORD_SPEC(AnimationID.BGS_SPECIAL_PLAYER, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // tested bgs spec
    MELEE_BANDOS_GODSWORD_OR_SPEC(AnimationID.BGS_SPECIAL_ORNATE_PLAYER, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // verified 22/06/2024, assumed due to ags(or)
    MELEE_ARMADYL_GODSWORD_SPEC(AnimationID.AGS_SPECIAL_PLAYER, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // tested ags spec
    MELEE_ARMADYL_GODSWORD_OR_SPEC(AnimationID.AGS_SPECIAL_ORNATE_PLAYER, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // tested ags(or) spec
    MELEE_SCYTHE(AnimationID.SCYTHE_OF_VITUR_ATTACK, CombatSkill.MELEE), // tested w/ all scythe styles (so could be crush, but unlikely)
    MELEE_GHAZI_RAPIER_STAB(AnimationID.GHRAZI_RAPIER_ATTACK, CombatSkill.MELEE), // rapier slash is 390, basic slash animation. Also VLS stab.
    MELEE_ANCIENT_GODSWORD_SPEC(AnimationID.NGS_SPECIAL_PLAYER, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d),
    MELEE_CRYSTAL_HALBERD_SPEC(AnimationID.DRAGON_HALBERD_SPECIAL_ATTACK, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d),
    MELEE_SOULREAPER_AXE(AnimationID.ANCIENT_AXE_CRUSH, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d),
    MELEE_SOULREAPER_AXE_SPEC(AnimationID.ANCIENT_AXE_SPECIAL, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d),
    MELEE_GUTHANS_LUNGE(AnimationID.BARROWS_WAR_SPEAR_STAB, CombatSkill.MELEE),
    MELEE_GUTHANS_SWIPE(AnimationID.BARROWS_WAR_SPEAR_SLASH, CombatSkill.MELEE),
    MELEE_GUTHANS_POUNDMA(AnimationID.BARROWS_WAR_SPEAR_CRUSH, CombatSkill.MELEE),
    MELEE_TORAG_HAMMERS(AnimationID.BARROW_TORAG_CRUSH, CombatSkill.MELEE),
    MELEE_VERACS_FLAIL(AnimationID.BARROW_GUTHAN_CRUSH, CombatSkill.MELEE),
    MELEE_BLISTERWOOD_FLAIL_CRUSH(AnimationID.IVANDIS_FLAIL_ATTACK, CombatSkill.MELEE), // blisterwood flail
    MELEE_BONE_DAGGER_SPEC(AnimationID.DTTD_PLAYER_STAB_BONE_DAGGER, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // tested with all poison variants (p, p+, p++, none)
    MELEE_DUAL_MACUAHUITL(AnimationID.PMOON_MACUAHUITL_CRUSH, CombatSkill.MELEE), // https://oldschool.runescape.wiki/w/Dual_macuahuitl set effect needs custom code
    MELEE_BLUE_MOON_SPEAR_SPEC(AnimationID.HUMAN_ZAMORAKSPEAR_LUNGE, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // https://oldschool.runescape.wiki/w/Blue_moon_spear
    MELEE_BLUE_MOON_SPEAR(AnimationID.HUMAN_ZAMORAKSPEAR_STAB, CombatSkill.MELEE),
    MELEE_DHINS(AnimationID.HUMAN_DINHS_BULWARK_BASH, CombatSkill.MELEE), // https://oldschool.runescape.wiki/w/Dinh%27s_bulwark
    MELEE_URSINE_CHAINMACE_SPEC(AnimationID.HUMAN_SPECIAL02_URSINE, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // https://oldschool.runescape.wiki/w/Ursine_chainmace#Charged
    MELEE_ANCIENT_MACE_SPEC(AnimationID.SLICE_PLAYER_MACE_SPECIAL_ATTACK, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // https://oldschool.runescape.wiki/w/Ancient_mace
    MELEE_DSCIM_SPEC(AnimationID.SP_ATTACK_DRAGON_SCIMITAR, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // https://oldschool.runescape.wiki/w/Dragon_scimitar
    MELEE_D2H_SPEC(AnimationID.DRAGON_TWO_HANDED_SWORD, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // https://oldschool.runescape.wiki/w/Dragon_2h_sword
    MELEE_ARCLIGHT_SPEC(AnimationID.DARK_SPEC_PLAYER, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // https://oldschool.runescape.wiki/w/Arclight
    MELEE_SARA_SWORD_SPEC(AnimationID.SARADOMIN_SWORD_SPECIAL_PLAYER, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // https://oldschool.runescape.wiki/w/Saradomin_sword assumed to be the same for the blessed version
    MELEE_RED_KERIS_SPEC(AnimationID.TOA_KERIS_PARTISAN_SPECIAL01, CombatSkill.MELEE, MetaData.SPECIAL_ATTACK.d), // https://oldschool.runescape.wiki/w/Keris_partisan_of_corruption
    MELEE_SALAMANDER(AnimationID.HUMAN_ATTACK_SALAMANDER, CombatSkill.MELEE), // https://oldschool.runescape.wiki/w/Salamander
    MELEE_INFERNAL_TECPATL(AnimationID.TECPATL_STAB, CombatSkill.MELEE), // https://oldschool.runescape.wiki/w/Infernal_tecpatl
    MELEE_HALLOWED_FLAIL(AnimationID.HUMAN_WEAPONS_HALLOWED_FLAIL01_ATTACK01, CombatSkill.MELEE), // https://oldschool.runescape.wiki/w/Hallowed_flail
    MELEE_FELLING_AXE(AnimationID.FORESTRY_2H_AXE_ATTACK, CombatSkill.MELEE), // https://oldschool.runescape.wiki/w/Crystal_felling_axe
    MELEE_HALLOWFELL(AnimationID.HUMAN_HALLOWFELL_SLASH, CombatSkill.MELEE),
    MELEE_OBLIGATOR(AnimationID.TFA_HUMAN_OBLIGATOR_ATTACK, CombatSkill.MELEE), // The Fractured Archive, 3t
    MELEE_OBLIGATOR_MULTIHIT(AnimationID.TFA_HUMAN_OBLIGATOR_MULTIHIT, CombatSkill.MELEE), // 5t
    MELEE_OBLIGATOR_FEMALE(AnimationID.TFA_HUMAN_OBLIGATOR_ATTACK_FEMALE, CombatSkill.MELEE),
    MELEE_OBLIGATOR_MULTIHIT_FEMALE(AnimationID.TFA_HUMAN_OBLIGATOR_MULTIHIT_FEMALE, CombatSkill.MELEE),

    // RANGED
    RANGED_CHINCHOMPA(AnimationID.HUMAN_CHINCHOMPA_ATTACK_PVN, CombatSkill.RANGED),
    RANGED_SHORTBOW(AnimationID.HUMAN_BOW, CombatSkill.RANGED, MetaData.STANDARD_BOW_ATTACK.d), // Confirmed same w/ 3 types of arrows, w/ maple, magic, & hunter's shortbow, craw's bow, dbow, dbow spec
    RANGED_RUNE_KNIFE_PVP(AnimationID.HUMAN_STAKE2, CombatSkill.RANGED), // 1 tick animation, has 1 tick delay between attacks. likely same for all knives. Same for morrigan's javelins, both spec & normal attack.
    RANGED_MAGIC_SHORTBOW_SPEC(AnimationID.SNAPSHOT, CombatSkill.RANGED, MetaData.STANDARD_BOW_ATTACK.d|MetaData.SPECIAL_ATTACK.d),
    RANGED_CROSSBOW_PVP(AnimationID.XBOWS_HUMAN_FIRE_AND_RELOAD, CombatSkill.RANGED), // Tested RCB & ACB w/ dragonstone bolts (e) & diamond bolts (e)
    RANGED_BLOWPIPE(AnimationID.SNAKEBOSS_BLOWPIPE_ATTACK, CombatSkill.RANGED), // tested in PvP with all styles. Has 1 tick delay between animations in pvp.
    RANGED_DARTS(AnimationID.II_HUMAN_DART_THROW_PVN, CombatSkill.RANGED), // tested w/ addy darts. Seems to be constant animation but sometimes stalls and doesn't animate
    RANGED_BALLISTA(AnimationID.BALLISTA_ATTACK, CombatSkill.RANGED), // Tested w/ dragon javelins.
    RANGED_BALLISTA_SPEC(AnimationID.BALLISTA_SPECIAL_ATTACK_PVN, CombatSkill.RANGED, MetaData.SPECIAL_ATTACK.d),
    RANGED_RUNE_THROWNAXE_SPEC(AnimationID.CHAINHIT, CombatSkill.RANGED, MetaData.SPECIAL_ATTACK.d), // https://oldschool.runescape.wiki/w/Rune_thrownaxe
    RANGED_DRAGON_THROWNAXE_SPEC(AnimationID.HUMAN_DRAGON_TAXE_SPEC, CombatSkill.RANGED, MetaData.SPECIAL_ATTACK.d),
    RANGED_RUNE_CROSSBOW(AnimationID.XBOWS_HUMAN_FIRE_AND_RELOAD_PVN, CombatSkill.RANGED),
    RANGED_RUNE_CROSSBOW_OR(AnimationID.HUMAN_XBOWS_LEAGUE03_ATTACK_PVN, CombatSkill.RANGED),
    RANGED_BALLISTA_2(AnimationID.BALLISTA_ATTACK_PVN, CombatSkill.RANGED), // tested w/ light & heavy ballista, dragon & iron javelins.
    RANGED_RUNE_KNIFE(AnimationID.HUMAN_STAKE2_PVN, CombatSkill.RANGED), // 1 tick animation, has 1 tick delay between attacks. Also d thrownaxe
    RANGED_DRAGON_KNIFE(AnimationID.HUMAN_DRAGON_KNIFE, CombatSkill.RANGED),
    RANGED_DRAGON_KNIFE_SPEC(AnimationID.HUMAN_DRAGON_TKNIVES_SPEC, CombatSkill.RANGED, MetaData.SPECIAL_ATTACK.d),
    RANGED_DRAGON_KNIFE_POISONED(AnimationID.HUMAN_DRAGON_KNIFE_P, CombatSkill.RANGED), // tested w/ d knife p++
    RANGED_DRAGON_KNIFE_POISONED_SPEC(AnimationID.HUMAN_DRAGON_TKNIVES_SPEC_POISON, CombatSkill.RANGED, MetaData.SPECIAL_ATTACK.d),
    RANGED_ZARYTE_CROSSBOW(AnimationID.ZCB_ATTACK_PVN, CombatSkill.RANGED),
    RANGED_ZARYTE_CROSSBOW_PVP(AnimationID.ZCB_ATTACK, CombatSkill.RANGED),
    RANGED_BLAZING_BLOWPIPE(AnimationID.SNAKEBOSS_BLOWPIPE_ATTACK_ORNAMENT, CombatSkill.RANGED),
    RANGED_VENATOR_BOW(AnimationID.HUMAN_WEAPON_BOW_VENATOR01_SHOOT, CombatSkill.RANGED, MetaData.STANDARD_BOW_ATTACK.d),
    RANGED_KARIL_CROSSBOW(AnimationID.BARROWS_REPEATING_CROSSBOW_FIRE, CombatSkill.RANGED),
    RANGED_ATLATL(AnimationID.HUMAN_ATLATL_ATTACK_RANGED_01, CombatSkill.RANGED, MetaData.STANDARD_BOW_ATTACK.d), // https://oldschool.runescape.wiki/w/Eclipse_atlatl
    RANGED_ATLATL_SPEC(AnimationID.HUMAN_SPECIAL_ATLATL_01, CombatSkill.RANGED, MetaData.STANDARD_BOW_ATTACK.d|MetaData.SPECIAL_ATTACK.d),
    RANGED_TONALZTICS(AnimationID.HUMAN_GLAIVE_RALOS01_CHARGED_THROW, CombatSkill.RANGED), // https://oldschool.runescape.wiki/w/Tonalztics_of_ralos#Charged
    RANGED_TONALZTICS_SPEC(AnimationID.HUMAN_GLAIVE_RALOS01_CHARGED_SPECIAL, CombatSkill.RANGED, MetaData.SPECIAL_ATTACK.d),
    RANGED_WEBWEAVER_SPEC(AnimationID.HUMAN_SPECIAL01_WEBWEAVER, CombatSkill.RANGED, MetaData.STANDARD_BOW_ATTACK.d|MetaData.SPECIAL_ATTACK.d), // https://oldschool.runescape.wiki/w/Webweaver_bow#Charged
    RANGED_BONE_CROSSBOW_SPEC(AnimationID.DTTD_PLAYER_FIRE_BONE_CROSSBOW_PVN, CombatSkill.RANGED, MetaData.SPECIAL_ATTACK.d), // https://oldschool.runescape.wiki/w/Dorgeshuun_crossbow
    RANGED_SCORCHING_BOW_SPEC(AnimationID.HUMAN_WEAPON_BOW_SCORCHED_01_SPEC, CombatSkill.RANGED, MetaData.STANDARD_BOW_ATTACK.d|MetaData.SPECIAL_ATTACK.d), // https://oldschool.runescape.wiki/w/Scorching_bow
    RANGED_ASCENSION_CROSSBOW_LEFT(AnimationID.TFA_HUMAN_ASCENSION_CROSSBOW_FIRE_LEFT, CombatSkill.RANGED), // The Fractured Archive, 3t (2t on rapid)
    RANGED_ASCENSION_CROSSBOW_LEFT_PVN(AnimationID.TFA_HUMAN_ASCENSION_CROSSBOW_FIRE_LEFT_PVN, CombatSkill.RANGED),
    RANGED_ASCENSION_CROSSBOW_RIGHT(AnimationID.TFA_HUMAN_ASCENSION_CROSSBOW_FIRE_RIGHT, CombatSkill.RANGED),
    RANGED_ASCENSION_CROSSBOW_RIGHT_PVN(AnimationID.TFA_HUMAN_ASCENSION_CROSSBOW_FIRE_RIGHT_PVN, CombatSkill.RANGED),

    // MAGIC - Keep in spellbook order (staves last) then alphabetical order and oneline
    MAGIC_GOD_SPELL(AnimationID.HUMAN_CASTING, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d), // https://oldschool.runescape.wiki/w/God_spells
    MAGIC_IBAN_BLAST(AnimationID.HUMAN_CASTIBANBLAST, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_SLAYER_DART(AnimationID.SLAYER_MAGICDART_CAST, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d), // https://oldschool.runescape.wiki/w/Magic_Dart
    MAGIC_STANDARD_BIND(AnimationID.HUMAN_CASTENTANGLE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d), // tested w/ bind, snare, entangle
    MAGIC_STANDARD_BIND_STAFF(AnimationID.HUMAN_CASTENTANGLE_STAFF, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d), // tested w/ bind, snare, entangle, various staves
    MAGIC_STANDARD_CONFUSE(AnimationID.HUMAN_CASTCONFUSE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_CONFUSE_STAFF(AnimationID.HUMAN_CASTCONFUSE_STAFF, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_CONFUSE_STAFF_WALK(AnimationID.HUMAN_CASTCONFUSE_STAFF_WALKMERGE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_CONFUSE_WALK(AnimationID.HUMAN_CASTCONFUSE_WALKMERGE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_CRUMBLE_UNDEAD(AnimationID.HUMAN_CASTCRUMBLEUNDEAD, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_CRUMBLE_UNDEAD_HOLDING_STAFF(AnimationID.HUMAN_CASTCRUMBLEUNDEAD_STAFF, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_ENFEEBLE(AnimationID.HUMAN_CASTENFEEBLE_STAFF, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_STRIKE_BOLT_BLAST(AnimationID.HUMAN_CASTSTRIKE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_STRIKE_BOLT_BLAST_STAFF(AnimationID.HUMAN_CASTSTRIKE_STAFF, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_STRIKE_BOLT_BLAST_STAFF_WALK(AnimationID.HUMAN_CASTSTRIKE_STAFF_WALKMERGE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d), // strike, bolt and blast (tested all spells, different weapons)
    MAGIC_STANDARD_STRIKE_BOLT_BLAST_WALK(AnimationID.HUMAN_CASTSTRIKE_WALKMERGE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d), // tested w/ bolt
    MAGIC_STANDARD_STUN(AnimationID.HUMAN_CASTSTUN_STAFF, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_SURGE(AnimationID.HUMAN_CAST_SURGE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d), // tested many staves
    MAGIC_STANDARD_SURGE_STAFF(AnimationID.HUMAN_CAST_SURGE_FAST, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d), // tested many staves
    MAGIC_STANDARD_SURGE_STAFF_WALK(AnimationID.HUMAN_CAST_SURGE_WALKMERGE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d), // tested many staves
    MAGIC_STANDARD_VULNERABILITY_CURSE(AnimationID.HUMAN_CASTCURSE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_VULNERABILITY_CURSE_STAFF(AnimationID.HUMAN_CASTCURSE_STAFF, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_VULNERABILITY_CURSE_STAFF_WALK(AnimationID.HUMAN_CASTCURSE_STAFF_WALKMERGE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_VULNERABILITY_CURSE_WALK(AnimationID.HUMAN_CASTCURSE_WALKMERGE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_WAVE(AnimationID.HUMAN_CASTWAVE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d), // tested w/ wave spells
    MAGIC_STANDARD_WAVE_STAFF(AnimationID.HUMAN_CASTWAVE_STAFF, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d), // tested many staves
    MAGIC_STANDARD_WAVE_STAFF_WALK(AnimationID.HUMAN_CASTWAVE_STAFF_WALKMERGE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d), // tested many staves
    MAGIC_STANDARD_WAVE_WALK(AnimationID.HUMAN_CASTWAVE_WALKMERGE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d), // tested w/ wave spells
    MAGIC_STANDARD_WEAKEN(AnimationID.HUMAN_CASTWEAKEN, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_WEAKEN_STAFF(AnimationID.HUMAN_CASTWEAKEN_STAFF, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_WEAKEN_STAFF_WALK(AnimationID.HUMAN_CASTWEAKEN_STAFF_WALKMERGE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),
    MAGIC_STANDARD_WEAKEN_WALK(AnimationID.HUMAN_CASTWEAKEN_WALKMERGE, CombatSkill.MAGIC, Spellbook.STANDARD, MetaData.NO_DATA.d),

    MAGIC_ANCIENT_MULTI_TARGET(AnimationID.ZAROS_VERTICAL_CASTING_WALKMERGE, CombatSkill.MAGIC, Spellbook.ANCIENT, MetaData.NO_DATA.d), // Burst & Barrage animations (tested all 8, different weapons)
    MAGIC_ANCIENT_MULTI_TARGET_PVP(AnimationID.ZAROS_VERTICAL_CASTING, CombatSkill.MAGIC, Spellbook.ANCIENT, MetaData.NO_DATA.d), // Burst & Barrage animations (tested all 8, different weapons)
    MAGIC_ANCIENT_SINGLE_TARGET(AnimationID.ZAROS_CASTING_WALKMERGE, CombatSkill.MAGIC, Spellbook.ANCIENT, MetaData.NO_DATA.d), // Rush & Blitz animations (tested all 8, different weapons)
    MAGIC_ANCIENT_SINGLE_TARGET_PVP(AnimationID.ZAROS_CASTING, CombatSkill.MAGIC, Spellbook.ANCIENT, MetaData.NO_DATA.d), // Rush & Blitz animations

    MAGIC_ARCEUUS_DEMONBANE(AnimationID.HUMAN_SPELLCAST_DEMONBANE, CombatSkill.MAGIC, Spellbook.ARCEUUS, MetaData.NO_DATA.d), // Also greater corruption, so that may accidentally trigger a manual-cast, but that's probably fine only affects Muspah
    MAGIC_ARCEUUS_GRASP(AnimationID.HUMAN_SPELLCAST_GRASP, CombatSkill.MAGIC, Spellbook.ARCEUUS, MetaData.NO_DATA.d),

    MAGIC_ACCURSED_SCEPTRE_SPEC(AnimationID.HUMAN_SPECIAL_ACCURSED, CombatSkill.MAGIC, MetaData.SPECIAL_ATTACK.d),
    MAGIC_EYE_OF_AYAK(AnimationID.HUMAN_EYE_OF_AYAK_NORMAL, CombatSkill.MAGIC, MetaData.NO_DATA.d),
    MAGIC_EYE_OF_AYAK_SPEC(AnimationID.HUMAN_EYE_OF_AYAK_SPECIAL, CombatSkill.MAGIC, MetaData.SPECIAL_ATTACK.d), // https://github.com/ngraves95/attacktimer/issues/91
    MAGIC_TUMEKENS_SHADOW(AnimationID.TOA_SOT_CAST_B, CombatSkill.MAGIC, MetaData.NO_DATA.d),
    MAGIC_VOLATILE_NIGHTMARE_STAFF_SPEC(AnimationID.NIGHTMARE_STAFF_SPECIAL, CombatSkill.MAGIC, MetaData.SPECIAL_ATTACK.d), // assume 99 mage's base damage (does not rise when boosted).
    MAGIC_WARPED_SCEPTRE(AnimationID.POG_WARPED_SCEPTRE_ATTACK, CombatSkill.MAGIC, MetaData.NO_DATA.d), // https://oldschool.runescape.wiki/w/Warped_sceptre
    MAGIC_ZORYA_TOME(AnimationID.TFA_HUMAN_ZORYA_TOME_ATTACK, CombatSkill.MAGIC, MetaData.NO_DATA.d), // The Fractured Archive, 3t
    MAGIC_ZORYA_TOME_FEMALE(AnimationID.TFA_HUMAN_ZORYA_TOME_ATTACK_FEMALE, CombatSkill.MAGIC, MetaData.NO_DATA.d),
    MAGIC_ZORYA_TOME_SPEC(AnimationID.TFA_HUMAN_ZORYA_TOME_SPECIAL_ATTACK, CombatSkill.MAGIC, MetaData.SPECIAL_ATTACK.d), // next 3 attacks are 2t if the spec lands, 5 if two specs in a row land
    MAGIC_ZORYA_TOME_SPEC_FEMALE(AnimationID.TFA_HUMAN_ZORYA_TOME_SPECIAL_ATTACK_FEMALE, CombatSkill.MAGIC, MetaData.SPECIAL_ATTACK.d),

    // Misc
    MAGIC_IMBUE(AnimationID.HUMAN_CASTBONESTOBANANAS, CombatSkill.NON_ATTACK),
    SPELLBOOK_SWAP(AnimationID.DREAM_PLAYER_SPELLBOOK_SWAP, CombatSkill.NON_ATTACK),
    LUNAR_GROUP(AnimationID.QUEST_LUNAR_SPELL_CAST_SPELL_ON_GROUP, CombatSkill.NON_ATTACK), // heal group, cure group, etc
    LUNAR_OTHER(AnimationID.QUEST_LUNAR_PUSHING_MAGIC_ANIMATION, CombatSkill.NON_ATTACK), // Venge other, heal other, spec transfer, cure other, cure me, etc
    NPC_CONTACT(AnimationID.LUNAR_HUMAN_MAGIC_SUMMON2, CombatSkill.NON_ATTACK), // Also bake pie and pot share
    VENGEANCE_1(AnimationID.VENGEANCE_SPELL_ANIM_NOSTALLING, CombatSkill.NON_ATTACK),
    VENGEANCE_2(AnimationID.VENGEANCE_SPELL_ANIM_STALLING, CombatSkill.NON_ATTACK),
    REANIMATION(AnimationID.ARCEUUS_NECROMANCY_PLAYERANIM, CombatSkill.NON_ATTACK),
    DEMONIC_OFFERING(AnimationID.HUMAN_CAST_OFFERING, CombatSkill.NON_ATTACK), // Also sinister offering
    SHADOW_VEIL(AnimationID.HUMAN_SPELLCAST_SHADOWVEIL, CombatSkill.NON_ATTACK),
    MARK_OF_DARKNESS(AnimationID.HUMAN_CAST_SELFIMBUE, CombatSkill.NON_ATTACK), // Also death charge and ward of arceuss
    PICK_POCKETING(AnimationID.HUMAN_PICKPOCKET, CombatSkill.NON_ATTACK),
    SUMMON_THRALL(AnimationID.HUMAN_SPELLCAST_RESURRECT, CombatSkill.NON_ATTACK),
    LUNAR_TELEPORT(AnimationID.HUMAN_TELEPORT_OTHER_IMPACT, CombatSkill.NON_ATTACK),
    MONSTER_EXAMINE(AnimationID.DREAM_PLAYER_MONSTEREXAM_STATSPY, CombatSkill.NON_ATTACK), // Also stat spy
    HUMIDIFY(AnimationID.DREAM_PLAYER_HUMIDIFY_SPELL, CombatSkill.NON_ATTACK),
    GEOMANCY(AnimationID.LUNAR_HUMAN_MAGIC_GEOMANCY, CombatSkill.NON_ATTACK),
    DREAM(AnimationID.FOSSIL_LOC_CLAM_IDLE_SHUT, CombatSkill.NON_ATTACK),
    ROCKSLUG_BAG_OF_SALT(AnimationID.SLAYER_SALT_SPRINKLE, CombatSkill.NON_ATTACK), // https://oldschool.runescape.wiki/w/Rockslug
    DESSET_LIZARD_ICE_COOLER(AnimationID.HUMAN_CHINCHOMPA_ATTACK, CombatSkill.NON_ATTACK), // https://oldschool.runescape.wiki/w/Desert_Lizard
    // Tick manipulation actions are not trustworthy for attack starts, in fact for the most part they're like an eat and would stop attacks being allowed.
    FLECTHING_KNIFE(AnimationID.HUMAN_FLETCHING, CombatSkill.NON_ATTACK), // knife & log (celastrus bark, etc)
    FLECTHING_KEBBIT(AnimationID.HUMAN_CRAFTING_SPIKEDVAMBRACES, CombatSkill.NON_ATTACK), // kebbit & vamb
    FLECTHING_CHISEL(AnimationID.HUMAN_FLETCHING_HUNTINGBOLTS, CombatSkill.NON_ATTACK), // chisel & moonlight antler
    FLECTHING_DART_TIP_1(AnimationID.HUMAN_FLETCHING_ADD_DART_FEATHERS_BRONZE, CombatSkill.NON_ATTACK), // dart tip & feather
    FLECTHING_DART_TIP_2(AnimationID.HUMAN_FLETCHING_ADD_DART_FEATHERS_IRON, CombatSkill.NON_ATTACK), // dart tip & feather
    FLECTHING_DART_TIP_3(AnimationID.HUMAN_FLETCHING_ADD_DART_FEATHERS_STEEL, CombatSkill.NON_ATTACK), // dart tip & feather
    FLECTHING_DART_TIP_4(AnimationID.HUMAN_FLETCHING_ADD_DART_FEATHERS_MITHRIL, CombatSkill.NON_ATTACK), // dart tip & feather
    FLECTHING_DART_TIP_5(AnimationID.HUMAN_FLETCHING_ADD_DART_FEATHERS_ADAMANT, CombatSkill.NON_ATTACK), // dart tip & feather
    FLECTHING_DART_TIP_6(AnimationID.HUMAN_FLETCHING_ADD_DART_FEATHERS_RUNE, CombatSkill.NON_ATTACK), // dart tip & feather
    FLECTHING_DART_TIP_7(AnimationID.HUMAN_FLETCHING_ADD_DART_FEATHERS_DRAGON, CombatSkill.NON_ATTACK), // dart tip & feather
    FLECTHING_DART_TIP_8(AnimationID.HUMAN_FLETCHING_ADD_DART_FEATHERS_AMETHYST, CombatSkill.NON_ATTACK), // dart tip & feather
    HERB_TAR(AnimationID.HUMAN_SALAMANDER_TAR_GRIND, CombatSkill.NON_ATTACK), // pestle motar animation
    SETUP_HUNTER_TRAP(AnimationID.HUMAN_LAYTRAP, CombatSkill.NON_ATTACK),
    RESET_SNARE_TRAP(AnimationID.HUMAN_HUNTING_DISMANTLE_NET, CombatSkill.NON_ATTACK),
    RESET_BOX_TRAP(AnimationID.HUNTING_SETTING_TRAP_SMALL, CombatSkill.NON_ATTACK),

    DESERT_AMMY(AnimationID.TELEPORT_NARDAH_HUMAN, CombatSkill.NON_ATTACK),

    EAT_FOOD_OR_POTION(AnimationID.HUMAN_EAT, CombatSkill.NON_ATTACK),
    OVERLOAD_HIT(AnimationID.HUMAN_KILLERWATT_ELECTRICSHOCK, CombatSkill.NON_ATTACK), // https://oldschool.runescape.wiki/w/Overload_(Chambers_of_Xeric)#4_dose

    TAKING_HIT_1HANDED_UNARMED(AnimationID.HUMAN_AXE_BLOCK, CombatSkill.NON_ATTACK),
    TAKING_HIT_2H_SWORD(AnimationID.HUMAN_DHSWORD_BLOCK, CombatSkill.NON_ATTACK),
    TAKING_HIT_ANCHOR(AnimationID.BRAIN_PLAYER_ANCHOR_DEFEND, CombatSkill.NON_ATTACK),
    TAKING_HIT_BLISTERWOOD_FLAIL(AnimationID.IVANDIS_FLAIL_DEFEND, CombatSkill.NON_ATTACK),
    TAKING_HIT_BLOWPIPE(AnimationID.HUMAN_SPEAR_BLOCK, CombatSkill.NON_ATTACK),
    TAKING_HIT_BULWARK(AnimationID.HUMAN_DINHS_BULWARK_BLOCK, CombatSkill.NON_ATTACK),
    TAKING_HIT_CHAINMACE(AnimationID.WILD_CAVE_CHAINMACE_DEFEND, CombatSkill.NON_ATTACK),
    TAKING_HIT_CHIN_CHOMPA(AnimationID.HUMAN_CHINCHOMPA_DEFEND, CombatSkill.NON_ATTACK),
    TAKING_HIT_DAGGER(AnimationID.HUMAN_DDAGGER_BLOCK, CombatSkill.NON_ATTACK),
    TAKING_HIT_DEFENDER(AnimationID.WARGUILD_PARRY_DEFEND, CombatSkill.NON_ATTACK),
    TAKING_HIT_FANG(AnimationID.HUMAN_SWORD_DEF, CombatSkill.NON_ATTACK),
    TAKING_HIT_GODSWORD(AnimationID.DH_SWORD_UPDATE_DEFEND, CombatSkill.NON_ATTACK),
    TAKING_HIT_KERIS(AnimationID.HUMAN_DSPEAR_BLOCK, CombatSkill.NON_ATTACK),
    TAKING_HIT_LARGE_STAFF(AnimationID.HUMAN_STAFFORB_BLOCK, CombatSkill.NON_ATTACK),
    TAKING_HIT_MACE(AnimationID.HUMAN_BLUNT_BLOCK, CombatSkill.NON_ATTACK),
    TAKING_HIT_OBBY_MAUL(AnimationID.SLAYER_GRANITE_MAUL_DEFEND, CombatSkill.NON_ATTACK),
    TAKING_HIT_SCYTHE(AnimationID.HUMAN_SCYTHE_BLOCK, CombatSkill.NON_ATTACK),
    TAKING_HIT_SHIELD(AnimationID.HUMAN_SHIELD_DEFENCE, CombatSkill.NON_ATTACK),
    TAKING_HIT_SPEAR(AnimationID.HUMAN_ZAMORAKSPEAR_BLOCK, CombatSkill.NON_ATTACK),
    TAKING_HIT_STAFF(AnimationID.HUMAN_STAFF_BLOCK, CombatSkill.NON_ATTACK),
    TAKING_HIT_UNARMED(AnimationID.HUMAN_UNARMEDBLOCK, CombatSkill.NON_ATTACK),
    TAKING_HIT_VERACS_FLAIL(AnimationID.BARROW_GUTHAN_DEFEND, CombatSkill.NON_ATTACK),
    TAKING_HIT_WHIP(AnimationID.SLAYER_ABYSSAL_WHIP_DEFEND, CombatSkill.NON_ATTACK),
    TAKING_HIT_KISTEN(AnimationID.HUMAN_WEAPONS_CRIMSON_KISTEN_DEF, CombatSkill.NON_ATTACK),
    TAKING_HIT_HALLOWFELL(AnimationID.HUMAN_HALLOWFELL_DEFEND, CombatSkill.NON_ATTACK),
    TAKING_HIT_OBLIGATOR(AnimationID.TFA_HUMAN_OBLIGATOR_DEF, CombatSkill.NON_ATTACK),
    TAKING_HIT_OBLIGATOR_FEMALE(AnimationID.TFA_HUMAN_OBLIGATOR_DEF_FEMALE, CombatSkill.NON_ATTACK),
    TAKING_HIT_ZORYA_TOME(AnimationID.TFA_HUMAN_ZORYA_TOME_DEFEND, CombatSkill.NON_ATTACK),
    TAKING_HIT_ZORYA_TOME_FEMALE(AnimationID.TFA_HUMAN_ZORYA_TOME_DEFEND_FEMALE, CombatSkill.NON_ATTACK),

    LOW_ALCH(AnimationID.HUMAN_CASTLOWLVLALCHEMY, CombatSkill.NON_ATTACK),
    HIGH_ALCH(AnimationID.HUMAN_CASTHIGHLVLALCHEMY, CombatSkill.NON_ATTACK),

    // Sailing
    SAILING_CANNON_ATTACK(AnimationID.HUMAN_SAILING_CANNON01_SMALL01_ATTACK01, CombatSkill.SAILING),
    // Sailing - not-attacks
    SAILING_CANNON_IDLE(AnimationID.HUMAN_SAILING_CANNON01_SMALL01_IDLE01, CombatSkill.NON_ATTACK),
    SAILING_CANNON_ENGAGE(AnimationID.HUMAN_SAILING_CANNON01_SMALL01_ENGAGE01, CombatSkill.NON_ATTACK),
    SAILING_HELM_1X3(AnimationID.HUMAN_SAILING_ALPHA_HELM_RAFT01_ACTIVE01, CombatSkill.NON_ATTACK),
    SAILING_HELM_2X5(AnimationID.HUMAN_SAILING_ALPHA_HELM_SMALL01_ACTIVE01, CombatSkill.NON_ATTACK),
    SAILING_HELM_3X8(AnimationID.HUMAN_SAILING_HELM_3X8_ACTIVE01, CombatSkill.NON_ATTACK),
    SAILING_HELM_LOOP_1X3(AnimationID.HUMAN_SAILING_ALPHA_HELM_RAFT01_ACTIVE01_LOOP, CombatSkill.NON_ATTACK),
    SAILING_HELM_LOOP_2X5(AnimationID.HUMAN_SAILING_ALPHA_HELM_SMALL01_ACTIVE01_LOOP, CombatSkill.NON_ATTACK),
    SAILING_HELM_LOOP_3X8(AnimationID.HUMAN_SAILING_HELM_3X8_ACTIVE01_LOOP, CombatSkill.NON_ATTACK),
    SAILING_TRIM_1_1X3(AnimationID.HUMAN_SAILING_ALPHA_HELM_RAFT01_TRIM_SAILS01, CombatSkill.NON_ATTACK),
    SAILING_TRIM_1_2X5(AnimationID.HUMAN_SAILING_ALPHA_HELM_SMALL01_TRIM_SAILS01, CombatSkill.NON_ATTACK),
    SAILING_TRIM_1_3X8(AnimationID.HUMAN_SAILING_HELM_3X8_TRIM_SAILS01, CombatSkill.NON_ATTACK),
    SAILING_TRIM_2_1X3(AnimationID.HUMAN_SAILING_ALPHA_HELM_RAFT01_TRIM_SAILS02, CombatSkill.NON_ATTACK),
    SAILING_TRIM_2_2X5(AnimationID.HUMAN_SAILING_ALPHA_HELM_SMALL01_TRIM_SAILS02, CombatSkill.NON_ATTACK),
    SAILING_TRIM_2_3X8(AnimationID.HUMAN_SAILING_HELM_3X8_TRIM_SAILS02, CombatSkill.NON_ATTACK),
    SAILING_TRIM_LOOP_1X3(AnimationID.HUMAN_SAILING_ALPHA_HELM_RAFT01_TRIM_SAILS01_LOOP, CombatSkill.NON_ATTACK),
    SAILING_TRIM_LOOP_2X5(AnimationID.HUMAN_SAILING_ALPHA_HELM_SMALL01_TRIM_SAILS01_LOOP, CombatSkill.NON_ATTACK),
    SAILING_TRIM_LOOP_3X8(AnimationID.HUMAN_SAILING_HELM_3X8_TRIM_SAILS01_LOOP, CombatSkill.NON_ATTACK),
    ;

    private static final Map<Integer, AnimationData> DATA;
    private static final Map<Spellbook, Set<AnimationData>> SPELL_BOOK_ANIMATIONS;
    private static final Map<Integer, AnimationData> NOT_ATTACKS;
    private static final Set<Integer> STANDARD_BOW_ATTACKS;

    public final int animationId;
    public final long metaData;
    public final CombatSkill combatSkill;
    @Getter
    private final Spellbook spellbook;

    // Simple animation data constructor for all melee, range and non attacks
    AnimationData(int animationId, CombatSkill combatSkill)
    {
        if (combatSkill == null)
        {
            throw new InvalidParameterException("Attack Style must be valid for AnimationData");
        }
        this.animationId = animationId;
        this.combatSkill = combatSkill;
        this.metaData = MetaData.NO_DATA.d;
        this.spellbook = null;
    }

    // Simple animation data constructor for all melee, range, magic attacks with extra data
    AnimationData(int animationId, CombatSkill combatSkill, long data)
    {
        if (combatSkill == null)
        {
            throw new InvalidParameterException("Attack Style must be valid for AnimationData");
        }
        this.animationId = animationId;
        this.combatSkill = combatSkill;
        this.metaData = data;
        this.spellbook = null;
    }

    // constructor for all magic attacks
    AnimationData(int animationId, CombatSkill combatSkill, Spellbook book, long data)
    {
        if (combatSkill == null)
        {
            throw new InvalidParameterException("Attack Style must be valid for AnimationData");
        }
        this.animationId = animationId;
        this.combatSkill = combatSkill;
        this.metaData = data;
        this.spellbook = book;
    }

    static
    {
        ImmutableMap.Builder<Integer, AnimationData> builder = new ImmutableMap.Builder<>();
        ImmutableMap.Builder<Integer, AnimationData> notAttacksBuilder = new ImmutableMap.Builder<>();
        Map<Spellbook, Set<AnimationData>> spellBookBuilder = new HashMap<>();
        ImmutableSet.Builder<Integer> standardBowBuilder = new ImmutableSet.Builder<Integer>();

        for (Spellbook s : Spellbook.values())
        {
            spellBookBuilder.put(s, new HashSet<AnimationData>());
        }

        for (AnimationData data : values())
        {
            builder.put(data.animationId, data);

            if (data.spellbook != null)
            {
                if (data.combatSkill != CombatSkill.MAGIC)
                {
                    throw new InvalidParameterException("Spell book should only be magic animations");
                }
                spellBookBuilder.get(data.spellbook).add(data);
            }
            if (data.combatSkill == CombatSkill.NON_ATTACK)
            {
                notAttacksBuilder.put(data.animationId, data);
            }

            if (MetaData.hasFlagSet(data.metaData, MetaData.STANDARD_BOW_ATTACK))
            {
                standardBowBuilder.add(data.animationId);
            }
            // Could also build a special attack map, but currently no use case.
        }

        DATA = builder.build();
        NOT_ATTACKS = notAttacksBuilder.build();
        SPELL_BOOK_ANIMATIONS = spellBookBuilder;
        STANDARD_BOW_ATTACKS = standardBowBuilder.build();
    }

    public static AnimationData fromId(int animationId)
    {
        return DATA.get(animationId);
    }

    public static Set<AnimationData> getAnimationsForSpellbook(Spellbook s)
    {
        return SPELL_BOOK_ANIMATIONS.get(s);
    }

    public static boolean isManualCasting(AnimationData animationData)
    {
        // This check ensures we don't treat staff animations which are magic attacks as a "manual cast".
        if (animationData != null && animationData.spellbook != null)
        {
            // We tell a manual cast by the animation data:
            return animationData.combatSkill == CombatSkill.MAGIC &&
                SPELL_BOOK_ANIMATIONS.get(animationData.spellbook).contains(animationData);
        }
        return false;
    }

    public static boolean isBlockListAnimation(int animationId)
    {
        return NOT_ATTACKS.containsKey(animationId);
    }

    @Override
    public String toString()
    {
        String[] words = super.toString().toLowerCase().split("_");
        Arrays.stream(words)
                .map(StringUtils::capitalize).collect(Collectors.toList()).toArray(words);

        return String.join(" ", words);
    }

    public boolean matchesSpellbook(Spellbook s)
    {
        if (this.spellbook != null)
        {
            return this.spellbook == s;
        }
        return false;
    }

    // isStandardBowAttack returns true if the animation is performed by a bow https://oldschool.runescape.wiki/w/Standard_ranged_weapons
    public boolean isStandardBowAttack()
    {
        return STANDARD_BOW_ATTACKS.contains(this.animationId);
    }

    public boolean isBlockListAnimation()
    {
        return NOT_ATTACKS.containsKey(this.animationId);
    }


    // An enum of combat skils that the animation is known to come from.
    //
    // This is used downstream to help determine things like is the player manually casting? Is this a powered
    // stave, etc.
    public enum CombatSkill
    {
        MELEE,
        RANGED,
        MAGIC,
        SAILING, // Specifically the "Combat Options" tab has been replaced and there is no traditional options.
        NON_ATTACK,
        ;

        @Override
        public String toString()
        {
            return StringUtils.capitalize(super.toString().toLowerCase());
        }
    }

    public enum MetaData
    {
        NO_DATA(),
        SPECIAL_ATTACK(1),
        STANDARD_BOW_ATTACK(2),
        ;

        MetaData(int bitshift)
        {
            this.d = 1 << bitshift;
        }
        MetaData()
        {
            this.d = 0;
        }

        public static boolean hasFlagSet(long input, MetaData data)
        {
            return (input & data.d) == data.d;
        }

        private final long d;

        @Override
        public String toString()
        {
            return StringUtils.capitalize(super.toString().toLowerCase());
        }
    }
}
