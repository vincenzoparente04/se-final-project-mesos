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

/**
 * Builder class for constructing {@link GameStateDto} objects from the game model.
 * 
 * This class serves as an adapter between the internal game model and the data transfer
 * objects (DTOs) sent to clients. It polymorphically converts model objects into their
 * corresponding DTO representations for network transmission.
 */
public class GameStateDtoBuilder {

    /**
     * Builds a complete game state DTO from the current game model.
     *
     * @param model the {@link GameModel} to extract state from
     * @return a {@link GameStateDto} representing the current game state
     */
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

    /**
     * Converts a player to a {@link PlayerDto}.
     *
     * @param player the player to convert
     * @return a {@link PlayerDto} representation of the player
     */
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

    /**
     * Converts an offer tile to an {@link OfferTileDto}.
     *
     * @param tile the offer tile to convert
     * @return an {@link OfferTileDto} representation of the tile
     */
    private static OfferTileDto toOfferTileDto(OfferTile tile) {
        String occupantName = tile.isOccupied() ? tile.getOccupant().getName() : null;
        ActionDetailsExtractor extractor = new ActionDetailsExtractor();
        tile.getAction().accept(extractor);
        return new OfferTileDto(tile.getLetter(), extractor.getLabel(), occupantName,
                extractor.getTopRowLimit(), extractor.getBottomRowLimit(),
                extractor.getTopRowUsed(), extractor.getBottomRowUsed());
    }

    /**
     * Converts turn order slots to {@link TurnOrderSlotDto} objects.
     *
     * @param slots the list of turn order slots to convert
     * @return a list of {@link TurnOrderSlotDto} representations
     */
    private static List<TurnOrderSlotDto> toTurnOrderSlotDtos(List<TurnOrderSlot> slots) {
        TurnOrderSlotDto[] dtos = new TurnOrderSlotDto[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            TurnOrderSlot slot = slots.get(i);
            String occupantName = slot.isOccupied() ? slot.getOccupant().getName() : null;
            dtos[i] = new TurnOrderSlotDto(i, occupantName);
        }
        return List.of(dtos);
    }

    /**
     * Converts a card to a {@link CardDto}.
     *
     * @param card the card to convert
     * @return a {@link CardDto} representation of the card
     */
    private static CardDto toCardDto(Card card) {
        CardToDtoVisitor visitor = new CardToDtoVisitor();
        card.accept(visitor);
        return visitor.getDto();
    }

    /**
     * {@link model.board.OfferTileAction.OfferTileActionVisitor} implementation to extract details from an offer tile's action for DTO construction.
     */
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

    /**
     * Builds a {@link CardDto} from a card by visiting it. Each card type maps to its own
     * presentation payload — subtype label, era and a short human-readable detail string —
     * so the rendering layer can display cards without knowing their domain classes.
     */
    private static class CardToDtoVisitor implements CardVisitor {
        private CardDto dto;

        @Override
        public void visit(HunterCard card) {
            dto = tribeDto(card, "HUNTER", card.hasTriggerIcon() ? " bonus on draw" : "no draw bonus");
        }

        @Override
        public void visit(ShamanCard card) {
            dto = tribeDto(card, "SHAMAN", "★".repeat(card.getStarCount()));
        }

        @Override
        public void visit(BuilderCard card) {
            dto = tribeDto(card, "BUILDER",
                    "-" + card.getBuilderDiscount() + " food/bldg  |  +" + card.getPrestigePoints() + " PP end");
        }

        @Override
        public void visit(ArtistCard card) {
            dto = tribeDto(card, "ARTIST", "end: +10PP per 2 artists");
        }

        @Override
        public void visit(InventorCard card) {
            dto = tribeDto(card, "INVENTOR", "icon: " + card.getInventionIcon().name());
        }

        @Override
        public void visit(GathererCard card) {
            dto = tribeDto(card, "GATHERER", "sustenance: -3 food");
        }

        /**
         * Fallback for any character role not handled by a dedicated overload. Reaching this
         * method signals a concrete {@link CharacterCard} subtype was added without a matching
         * visit overload, so it fails fast rather than emitting a mislabeled DTO.
         *
         * @param card the unhandled character card
         * @throws IllegalStateException always, naming the offending subtype
         */
        @Override
        public void visit(CharacterCard card) {
            throw new IllegalStateException("Unhandled CharacterCard subtype: " + card.getClass().getSimpleName());
        }

        @Override
        public void visit(EventCard card) {
            dto = eventDto(card);
        }

        @Override
        public void visit(SustenanceEventCard card) {
            dto = eventDto(card);
        }

        @Override
        public void visit(BuildingCard card) {
            dto = new CardDto(card.getId(), "BUILDING", eraOf(card),
                    card.getFoodCost(), card.getEndGamePoints(), card.getEffect().getDescription(),
                    card.getImagePath(), card.getBackImagePath());
        }

        /**
         * Builds a DTO for a tribe character card, which carries no food cost or end-game points.
         *
         * @param card    the character card to convert
         * @param subtype the display label identifying the character role
         * @param details the short human-readable detail string for this card
         * @return the populated {@link CardDto}
         */
        private static CardDto tribeDto(CharacterCard card, String subtype, String details) {
            return new CardDto(card.getId(), subtype, eraOf(card), 0, 0, details,
                    card.getImagePath(), card.getBackImagePath());
        }

        /**
         * Builds a DTO for an event card, which exposes no player-facing detail string.
         *
         * @param card the event card to convert
         * @return the populated {@link CardDto}
         */
        private static CardDto eventDto(EventCard card) {
            return new CardDto(card.getId(), "EVENT", eraOf(card), 0, 0, "",
                    card.getImagePath(), card.getBackImagePath());
        }

        /**
         * Renders the card era as a 1-based display string ("1", "2", "3").
         *
         * @param card the card whose era is formatted
         * @return the 1-based era label
         */
        private static String eraOf(Card card) {
            return String.valueOf(card.getEra().ordinal() + 1);
        }

        /**
         * @return the DTO produced by the most recent visit
         */
        CardDto getDto() {
            return dto;
        }
    }
}
