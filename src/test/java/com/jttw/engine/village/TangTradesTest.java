package com.jttw.engine.village;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The trade tables: every calling trades; every price can be asked and paid; nothing can be bought
 * for less than it sells for (no coin from nothing); the money-changer is at par; no mortal sells a
 * talisman, and the supernatural world's merchants do.
 */
class TangTradesTest {

    @Test
    void everyCallingTrades() {
        for (Calling c : Calling.values()) assertFalse(TangTrades.of(c).isEmpty(), c + " trades nothing");
    }

    @Test
    void everyPriceIsSaneAndPayable() {
        for (Calling c : Calling.values()) {
            for (TangTrades.Deal d : TangTrades.of(c)) {
                String what = c + " " + d;
                assertTrue(d.count() >= 1 && d.count() <= 64, what + ": a stack at most");
                assertTrue(d.maxUses() >= 1 && d.xp() >= 1, what + ": deals and pays experience");
                assertTrue(Coins.payable(d.wen()), what + ": a price in two stacks of coin");
                if (d.side() == TangTrades.Side.BUYS) assertTrue(Coins.payableInOneStack(d.wen()), what + ": paid out in one stack");
                if (d.pay() == TangTrades.Pay.COPPER) assertTrue(d.wen() <= 128, what + ": all-copper, two stacks at most");
                if (d.isMap()) assertTrue(TangTrades.MAP_PLACES.contains(d.item().substring(TangTrades.MAP.length())), what);
                else assertTrue(d.item().contains(":"), what + ": an item id");
            }
        }
    }

    @Test
    void nothingCanBeBoughtForLessThanItSells() {
        Map<String, Double> cheapest = new HashMap<>(), dearest = new HashMap<>();
        for (Calling c : Calling.values()) {
            for (TangTrades.Deal d : TangTrades.of(c)) {
                if (d.side() == TangTrades.Side.SELLS) cheapest.merge(d.item(), d.unit(), Math::min);
                else dearest.merge(d.item(), d.unit(), Math::max);
            }
        }
        dearest.forEach((item, paid) -> {
            Double cost = cheapest.get(item);
            if (cost != null) assertTrue(cost >= paid, item + " sells for " + cost + " a piece but is bought back for " + paid);
        });
    }

    @Test
    void theMoneyChangerIsAtPar() {
        List<TangTrades.Deal> changer = TangTrades.of(Calling.MERCHANT);
        for (String coin : List.of("jttw_engine:silver_tael", "jttw_engine:string_of_cash")) {
            int worth = coin.endsWith("silver_tael") ? Coins.COPPER_PER_TAEL : Coins.COPPER_PER_STRING;
            for (TangTrades.Side side : TangTrades.Side.values()) {
                TangTrades.Deal d = changer.stream().filter(x -> x.item().equals(coin) && x.side() == side).findFirst().orElseThrow();
                assertEquals(worth, d.wen(), coin + " " + side);
                assertEquals(TangTrades.Pay.COPPER, d.pay(), coin + " changed for copper");
            }
        }
    }

    @Test
    void talismansAreTheSupernaturalWorldsAlone() {
        for (Calling c : Calling.values()) {
            boolean sells = TangTrades.of(c).stream().anyMatch(d -> d.side() == TangTrades.Side.SELLS && TangTrades.isTalisman(d.item()));
            if (c.mortal()) assertFalse(sells, c + " is a mortal and sells talismans");
            else assertTrue(sells, c + " sells no talismans");
        }
        for (Calling c : Calling.values()) {
            for (TangTrades.Deal d : TangTrades.of(c)) {
                if (TangTrades.isTalisman(d.item())) assertTrue(d.wen() / d.count() >= 21, d + ": talismans are dear, priced in silver");
            }
        }
    }

    @Test
    void theScholarsMapEveryPlaceAndTheDearerTheFarther() {
        List<TangTrades.Deal> maps = TangTrades.of(Calling.SCHOLAR).stream().filter(TangTrades.Deal::isMap).toList();
        assertEquals(TangTrades.MAP_PLACES.size(), maps.size());
        for (int i = 1; i < maps.size(); i++) assertTrue(maps.get(i).wen() >= maps.get(i - 1).wen(), "a farther map costs no less");
    }

    @Test
    void mortalRemediesAreWeakerThanImmortalOnes() {
        assertTrue(VillageItems.PILL_SHARE < VillageItems.DEW_SHARE && VillageItems.DEW_SHARE < 1f, "a pill, then dew; never a whole pool");
        TangTrades.Deal pill = TangTrades.of(Calling.HERBALIST).stream().filter(d -> d.item().endsWith("qi_gathering_pill")).findFirst().orElseThrow();
        TangTrades.Deal dew = TangTrades.of(Calling.CELESTIAL_MERCHANT).stream().filter(d -> d.item().endsWith("jade_dew")).findFirst().orElseThrow();
        assertTrue(dew.wen() > pill.wen(), "Heaven's dew is dearer than a mortal pill");
    }
}
