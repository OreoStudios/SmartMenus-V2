package com.oreo.animation;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link AnimationController} slot ownership and multi-animation isolation without a live
 * inventory. A mocked {@link InventoryContents} records which slots the controller writes to.
 */
class AnimationControllerTest {

    /** Records the last item written per slot; captures {@code set(slot, item)} calls. */
    private static final class RecordingRenderer implements FrameRenderer {
        final List<Integer> renderedSlots = new ArrayList<>();

        @Override
        public ClickableItem render(int slot, com.oreo.gui.GuiItem item) {
            renderedSlots.add(slot);
            return null; // the controller still calls contents.set with null; that's fine here
        }
    }

    private static AnimationFrame frame(int... slots) {
        AnimationFrame.Builder b = AnimationFrame.builder();
        for (int slot : slots) b.set(slot, null);
        return b.build();
    }

    @Test
    void autoplayRegistersRunningAnimation() {
        GuiAnimation anim = GuiAnimation.builder().interval(1).autoplay(true)
                .frame(frame(0)).frame(frame(1)).build();
        AnimationController controller = new AnimationController();
        controller.register("border", anim);
        assertTrue(controller.isRunning("border"));
        assertTrue(controller.hasAny());
    }

    @Test
    void autoplayFalseStartsPaused() {
        GuiAnimation anim = GuiAnimation.builder().interval(1).autoplay(false)
                .frame(frame(0)).frame(frame(1)).build();
        AnimationController controller = new AnimationController();
        controller.register("border", anim);
        assertFalse(controller.isRunning("border"));
        controller.resume("border");
        assertTrue(controller.isRunning("border"));
    }

    @Test
    void tickAdvancesAndReportsCurrentFrame() {
        InventoryContents contents = mock(InventoryContents.class);
        GuiAnimation anim = GuiAnimation.builder().interval(1).autoplay(true)
                .frame(frame(0)).frame(frame(1)).frame(frame(2)).build();
        AnimationController controller = new AnimationController();
        controller.register("a", anim);

        controller.tick(contents, (slot, item) -> null);
        assertEquals(1, controller.currentFrame("a").orElseThrow());
        controller.tick(contents, (slot, item) -> null);
        assertEquals(2, controller.currentFrame("a").orElseThrow());
    }

    @Test
    void twoAnimationsRunIndependently() {
        InventoryContents contents = mock(InventoryContents.class);
        GuiAnimation fast = GuiAnimation.builder().interval(1).autoplay(true)
                .frame(frame(0)).frame(frame(1)).build();
        GuiAnimation slow = GuiAnimation.builder().interval(3).autoplay(true)
                .frame(frame(5)).frame(frame(6)).build();

        AnimationController controller = new AnimationController();
        controller.register("fast", fast);
        controller.register("slow", slow);

        controller.tick(contents, (slot, item) -> null);
        assertEquals(1, controller.currentFrame("fast").orElseThrow());
        assertEquals(0, controller.currentFrame("slow").orElseThrow()); // slow not yet advanced

        controller.tick(contents, (slot, item) -> null);
        controller.tick(contents, (slot, item) -> null);
        assertEquals(1, controller.currentFrame("slow").orElseThrow()); // now advanced at tick 3
    }

    @Test
    void pauseResumeStopControlNamedAnimation() {
        GuiAnimation anim = GuiAnimation.builder().interval(1).autoplay(true)
                .frame(frame(0)).frame(frame(1)).build();
        AnimationController controller = new AnimationController();
        controller.register("border", anim);

        controller.pause("border");
        assertFalse(controller.isRunning("border"));
        controller.resume("border");
        assertTrue(controller.isRunning("border"));
        controller.stop("border");
        assertFalse(controller.isRunning("border"));
    }

    @Test
    void ownedSlotsCoverEveryFrame() {
        GuiAnimation anim = GuiAnimation.builder().interval(1)
                .frame(frame(0, 1)).frame(frame(1, 2)).build();
        assertTrue(anim.getOwnedSlots().containsAll(List.of(0, 1, 2)));
    }
}
