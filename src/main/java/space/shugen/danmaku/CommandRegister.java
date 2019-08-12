package space.shugen.danmaku;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.cottonmc.clientcommands.ArgumentBuilders;
import io.github.cottonmc.clientcommands.ClientCommandPlugin;
import io.github.cottonmc.clientcommands.CottonClientCommandSource;
import net.minecraft.text.LiteralText;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class CommandRegister implements ClientCommandPlugin {
    public Main main;
    @Override
    public void registerCommands(CommandDispatcher<CottonClientCommandSource> commandDispatcher) {
        main=Main.getIt();
        commandDispatcher.register(ArgumentBuilders.literal("danmaku")
                .then(ArgumentBuilders.literal("connect")
                        .then(ArgumentBuilders.argument("roomid",integer())
                                .executes(context -> {
                                    main.Connect(IntegerArgumentType.getInteger(context,"roomid"));
                                    return 1;
                                }))
                        .executes(context -> {
                            context.getSource().sendFeedback(new LiteralText("你的房间号?"));
                            return 1;
                        }))
                .then(ArgumentBuilders.literal("disconnect")
                        .executes(context -> {
                            main.disconnect();
                            return 1;
                        }))
                .executes(context -> {
                    context.getSource().sendFeedback(new LiteralText("你想干啥？"));
                    return 1;
                }));
    }
}
