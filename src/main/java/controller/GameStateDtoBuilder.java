package controller;

import model.GameModel;
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

import java.util.List;

/**
 * Builds a {@link GameStateDto} snapshot from the current {@link GameModel} state.
 * Lives in the controller package so that {@link GameController} can use it
 * without depending on any server/network class.
 */
public class GameStateDtoBuilder {

    public static GameStateDto build(GameModel model) {
        Player currentPlayer = model.getCurrentPlayer();
        String currentEra = model.getCurrentEra() != null ? model.getCurrentEra().name() : null;
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

    // ─────────────────────────────────────────────────────────
    // Private visitor — maps each OfferTileAction to its label
    // and (for DRAW_CARDS) the per-row draw limits
    // ─────────────────────────────────────────────────────────

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

        String getLabel()           { return label; }
        Integer getTopRowLimit()    { return topRowLimit; }
        Integer getBottomRowLimit() { return bottomRowLimit; }
        Integer getTopRowUsed()     { return topRowUsed; }
        Integer getBottomRowUsed()  { return bottomRowUsed; }
    }

    // ─────────────────────────────────────────────────────────
    // Private visitor — maps each card type to a CardDto
    // without instanceof or downcast
    // ─────────────────────────────────────────────────────────

    private static class CardToDtoVisitor implements CardVisitor {
        private CardDto dto;

        @Override
        public void visit(CharacterCard card) {
            dto = new CardDto(card.getId(), "CHARACTER", card.getEra().name(), 0, 0);
        }

        @Override
        public void visit(EventCard card) {
            dto = new CardDto(card.getId(), "EVENT", card.getEra().name(), 0, 0);
        }

        @Override
        public void visit(SustenanceEventCard card) {
            dto = new CardDto(card.getId(), "EVENT", card.getEra().name(), 0, 0);
        }

        @Override
        public void visit(BuildingCard card) {
            dto = new CardDto(card.getId(), "BUILDING", card.getEra().name(), card.getFoodCost(), card.getEndGamePoints());
        }

        CardDto getDto() { return dto; }
    }
}
