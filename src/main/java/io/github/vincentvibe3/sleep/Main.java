package io.github.vincentvibe3.sleep;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("fabricmultisleep");

	private static final List<Block> beds = Arrays.asList(Blocks.BLACK_BED, Blocks.BLUE_BED, Blocks.BROWN_BED, Blocks.LIGHT_BLUE_BED, Blocks.CYAN_BED,
			Blocks.GRAY_BED, Blocks.GREEN_BED, Blocks.LIGHT_GRAY_BED, Blocks.LIME_BED, Blocks.MAGENTA_BED,
			Blocks.ORANGE_BED, Blocks.PINK_BED, Blocks.PURPLE_BED, Blocks.RED_BED, Blocks.WHITE_BED, Blocks.YELLOW_BED);

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	@Override
	public void onInitialize() {

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
		{
			BlockState state = world.getBlockState(hitResult.getBlockPos());
			Block block = state.getBlock();
			boolean isBed = false;
			for (Block bed:beds){
				if (block.equals(bed)){
					isBed = true;
					break;
				}
			}
			if (!player.isSpectator()&&isBed) {
				Runnable timeChangeTask = new Runnable() {

					final ServerWorld serverWorld = (ServerWorld) world;
					@Override
					public void run() {
						boolean ok = world.isNight()||world.isThundering();
						if (world.getPlayers().size()>1&&player.isSleeping()&&ok){
							Long nextDay = getNextdayTime(world.getTimeOfDay());
							serverWorld.setTimeOfDay(nextDay);
							serverWorld.setWeather(0, 0, false, false);
							List<ServerPlayerEntity> players = serverWorld.getServer().getPlayerManager().getPlayerList();
							for (ServerPlayerEntity serverPlayer:players){
								serverPlayer.sendMessage(Text.of("Good Morning Everyone"));
							}
						}
					}
				};
				Runnable sleepMessageTask = new Runnable() {
					final ServerWorld serverWorld = (ServerWorld) world;
					@Override
					public void run() {
						boolean ok = world.isNight()||world.isThundering();
						if (world.getPlayers().size()>1&&player.isSleeping()&&ok){
							List<ServerPlayerEntity> players = serverWorld.getServer().getPlayerManager().getPlayerList();
							for (ServerPlayerEntity serverPlayer:players){
								serverPlayer.sendMessage(Text.of("Good Night " + player.getDisplayName().getString() + " is sleeping"));
							}
						}
					}
				};
				executor.schedule(sleepMessageTask, 100, TimeUnit.MILLISECONDS);
				executor.schedule(timeChangeTask, 5050, TimeUnit.MILLISECONDS);
			}
			return ActionResult.PASS;
		});
		ServerLifecycleEvents.SERVER_STOPPING.register((server)-> executor.shutdown());
		LOGGER.info("fabricMultiSleep Ready");
	}

	private Long getNextdayTime(Long currentTime){
		long DAYTICKS = 24000L;
		long dayCount = currentTime / DAYTICKS;
		return (dayCount+1)*DAYTICKS;
	}
}
