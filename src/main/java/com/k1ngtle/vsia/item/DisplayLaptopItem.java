package com.k1ngtle.vsia.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DisplayLaptopItem extends Item {
    public DisplayLaptopItem(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack laptop =
                player.getItemInHand(
                        hand
                );

        InteractionHand driveHand =
                hand == InteractionHand.MAIN_HAND
                        ? InteractionHand.OFF_HAND
                        : InteractionHand.MAIN_HAND;

        ItemStack drive =
                player.getItemInHand(
                        driveHand
                );

        if (!(drive.getItem()
                instanceof DisplayHardDriveItem hardDrive)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.literal(
                                "Place a display hard drive in the other hand"
                        ),
                        true
                );
            }

            return InteractionResultHolder.sidedSuccess(
                    laptop,
                    level.isClientSide
            );
        }

        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                if (hardDrive.factoryProgrammed()) {
                    player.displayClientMessage(
                            Component.literal(
                                    "Factory Create Hard Drive is read-only"
                            ),
                            true
                    );
                } else {
                    DisplayHardDriveItem.clearProgram(
                            drive
                    );

                    player.displayClientMessage(
                            Component.literal(
                                    "Laptop erased display hard drive"
                            ),
                            true
                    );
                }
            } else if (hardDrive.factoryProgrammed()) {
                player.displayClientMessage(
                        Component.literal(
                                "Drive contains "
                                        + DisplayHardDriveItem.programName(
                                        drive
                                )
                        ),
                        true
                );
            } else {
                DisplayHardDriveItem.setProgramId(
                        drive,
                        DisplayHardDriveItem.PROGRAM_F35_CREATE
                );

                player.displayClientMessage(
                        Component.literal(
                                "Laptop wrote F-35 Create Display to hard drive"
                        ),
                        true
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(
                laptop,
                level.isClientSide
        );
    }
}
