package model;

import model.board.OfferTile;
import model.board.TurnOrderSlot;
import model.board.OfferTileAction.DrawCardsAction;
import model.board.OfferTileAction.OfferTileActionVisitor;
import model.board.OfferTileAction.TakeFoodAction;
import model.cards.Card;
import model.cards.buildingCards.BuildingCard;
import model.cards.characterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.cards.eventCards.SustenanceEventCard;
import model.player.Player;
import model.rowsManager.CardVisitor;
import shared.dto.CardDto;
import shared.dto.GameStateDto;
import shared.dto.OfferTileDto;
import shared.dto.PlayerDto;
import shared.dto.TribeDto;
import shared.dto.TurnOrderSlotDto;

import model.cards.characterCards.HunterCard;
import model.cards.characterCards.ShamanCard;
import model.cards.characterCards.BuilderCard;
import model.cards.characterCards.ArtistCard;
import model.cards.characterCards.InventorCard;
import model.cards.characterCards.GathererCard;

import java.util.List;

public class GameStateDtoBuilder {

    public static GameStateDto build(GameModel model) {
        Player currentPlayer = model.getCurrentPlayer();
        String currentEra = model.getCurrentEra() != null ? String.valueOf(model.getCurrentEra().ordinal() + 1) : null;
        String phase = model.getCurrentPhase() != null ? model.getCurrentPhase().name() : null;
        String currentPlayerName = currentPlayer != null ? currentPlayer.getName() : null;

        List<PlayerDto> players = model.getPlayers().stream()
                .map(GameStateDtoBuilder::toPlayerDto)
                .toList();

        List<OfferTileDto> offerTiles = model.getBoard().getOfferTiles().stream()
                .map(GameStateDtoBuilder::toOfferTileDto)
                .toList();

        List<TurnOrderSlotDto> turnOrderSlots = toTurnOrderSlotDtos(model.getBoard().getTurnOrderSlots());

        List<CardDto> topRowTribe = model.getRowsManager().getTopRowTribe().stream()
                .map(GameStateDtoBuilder::toCardDto)
                .toList();

        List<CardDto> bottomRowTribe = model.getRowsManager().getBottomRowTribe().stream()
                .map(GameStateDtoBuilder::toCardDto)
                .toList();

        List<CardDto> topRowBuilding = model.getRowsManager().getTopRowBuilding().stream()
                .map(GameStateDtoBuilder::toCardDto)
                .toList();

        List<CardDto> bottomRowBuilding = model.getRowsManager().getBottomRowBuilding().stream()
                .map(GameStateDtoBuilder::toCardDto)
                .toList();

        List<String> winners = model.getWinners().isEmpty() ? null : model.getWinners();

        return new GameStateDto(
                phase,
                currentPlayerName,
                model.getCurrentRound(),
                currentEra,
                players,
                offerTiles,
                turnOrderSlots,
                topRowTribe,
                bottomRowTribe,
                topRowBuilding,
                bottomRowBuilding,
                winners
        );
    }

    private static PlayerDto toPlayerDto(Player player) {
        String color = player.getColor() != null ? player.getColor().name() : null;
        String location = player.getLocation() != null ? player.getLocation().name() : null;

        List<CardDto> characterCards = player.getTribe().getAllCharacters().stream()
                .map(GameStateDtoBuilder::toCardDto)
                .toList();
        List<CardDto> buildings = player.getTribe().getBuildings().stream()
                .map(GameStateDtoBuilder::toCardDto)
                .toList();
        TribeDto tribeDto = new TribeDto(characterCards, buildings);

        return new PlayerDto(player.getName(), player.getFood(), player.getPrestigePoints(), color, location, tribeDto);
    }

    private static OfferTileDto toOfferTileDto(OfferTile tile) {
        String occupantName = tile.isOccupied() ? tile.getOccupant().getName() : null;
        ActionDetailsExtractor extractor = new ActionDetailsExtractor();
        tile.getAction().accept(extractor);
        return new OfferTileDto(tile.getLetter(), extractor.getLabel(), occupantName,
                extractor.getTopRowLimit(), extractor.getBottomRowLimit(),
                extractor.getTopRowUsed(), extractor.getBottomRowUsed());
    }

    private static List<TurnOrderSlotDto> toTurnOrderSlotDtos(List<TurnOrderSlot> slots) {
        TurnOrderSlotDto[] dtos = new TurnOrderSlotDto[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            TurnOrderSlot slot = slots.get(i);
            String occupantName = slot.isOccupied() ? slot.getOccupant().getName() : null;
            dtos[i] = new TurnOrderSlotDto(i, occupantName);
        }
        return List.of(dtos);
    }

    private static CardDto toCardDto(Card card) {
        CardToDtoVisitor visitor = new CardToDtoVisitor();
        card.accept(visitor);
        return visitor.getDto();
    }

    private static class ActionDetailsExtractor implements OfferTileActionVisitor {
        private String label;
        private Integer topRowLimit;
        private Integer bottomRowLimit;
        private Integer topRowUsed;
        private Integer bottomRowUsed;

        @Override
        public void visitDrawCards(DrawCardsAction action) {
            label = "DRAW_CARDS";
            topRowLimit = action.getMaxTopRowDraws();
            bottomRowLimit = action.getMaxBottomRowDraws();
            topRowUsed = action.getCurrentTopRowDraws();
            bottomRowUsed = action.getCurrentBottomRowDraws();
        }

        @Override
        public void visitTakeFood(TakeFoodAction action) {
            label = "TAKE_FOOD";
            topRowLimit = null;
            bottomRowLimit = null;
            topRowUsed = null;
            bottomRowUsed = null;
        }

        String getLabel() {
            return label;
        }

        Integer getTopRowLimit() {
            return topRowLimit;
        }

        Integer getBottomRowLimit() {
            return bottomRowLimit;
        }

        Integer getTopRowUsed() {
            return topRowUsed;
        }

        Integer getBottomRowUsed() {
            return bottomRowUsed;
        }
    }

    private static class CardToDtoVisitor implements CardVisitor {
        private CardDto dto;

        @Override
        public void visit(CharacterCard card) {
            String subtype = switch (card) {
                case HunterCard h -> "HUNTER";
                case ShamanCard s -> "SHAMAN";
                case BuilderCard b -> "BUILDER";
                case ArtistCard a -> "ARTIST";
                case InventorCard i -> "INVENTOR";
                case GathererCard g -> "GATHERER";
                default -> "CHARACTER";
            };
            String details = switch (card) {
                case HunterCard h -> h.hasTriggerIcon() ? " bonus on draw" : "no draw bonus";
                case ShamanCard s -> "★".repeat(s.getStarCount());
                case BuilderCard b ->
                        "-" + b.getBuilderDiscount() + " food/bldg  |  +" + b.getPrestigePoints() + " PP end";
                case InventorCard i -> "icon: " + i.getInventionIcon().name();
                case ArtistCard a -> "end: +10PP per 2 artists";
                case GathererCard g -> "sustenance: -3 food";
                default -> "";
            };
            dto = new CardDto(card.getId(), subtype, String.valueOf(card.getEra().ordinal() + 1),
                    0, 0, details, card.getImagePath(), card.getBackImagePath());
        }

        @Override
        public void visit(EventCard card) {
            dto = new CardDto(card.getId(), "EVENT", String.valueOf(card.getEra().ordinal() + 1), 0, 0, "", card.getImagePath(), card.getBackImagePath());
        }

        @Override
        public void visit(SustenanceEventCard card) {
            dto = new CardDto(card.getId(), "EVENT", String.valueOf(card.getEra().ordinal() + 1), 0, 0, "", card.getImagePath(), card.getBackImagePath());
        }

        @Override
        public void visit(BuildingCard card) {
            dto = new CardDto(card.getId(), "BUILDING", String.valueOf(card.getEra().ordinal() + 1), card.getFoodCost(), card.getEndGamePoints(), describeEffect(card.getEffectId()),
                    card.getImagePath(), card.getBackImagePath());
        }

        // only used to print the effect of the card in the cli
        private static String describeEffect(String effectId) {
            return switch (effectId) {
                case "shamanic_immunity"          -> "Ritual:no PP loss";
                case "shamanic_double_points"     -> "Ritual:dbl PP top";
                case "shamanic_extra_stars"       -> "Ritual:+3 stars";
                case "extra_food_on_totem_return" -> "Return:+1 food";
                case "extra_draw"                 -> "Post-act:draw 1";
                case "on_acquire_set"             -> "Acq:+5food/set";
                case "on_acquire_pair"            -> "Acq:+3food/pair";
                case "on_sustenance_artist"       -> "Sust:-1f/artist";
                case "on_sustenance_gatherer"     -> "Sust:-1f/gatherer";
                case "on_sustenance_inventor"     -> "Sust:-1f/inventor";
                case "on_hunt_bonus"              -> "Hunt:+1f+1PP";
                case "on_cave_paintings_bonus"    -> "Cave:+1f/artist";
                case "end_game_count_hunters"     -> "End:+3PP/hunter";
                case "end_game_count_shamans"     -> "End:+3PP/shaman";
                case "end_game_count_artists"     -> "End:+3PP/artist";
                case "end_game_count_inventors"   -> "End:+3PP/inventor";
                case "end_game_count_builders"    -> "End:+3PP/builder";
                case "end_game_count_gatherers"   -> "End:+3PP/gatherer";
                case "end_game_count_sets"        -> "End:+6PP/full set";
                case "end_game_double_builders"   -> "End:x2 builder PP";
                case "none"                       -> "";
                default                           -> effectId;
            };
        }

        CardDto getDto () {
            return dto;
        }
    }
}
