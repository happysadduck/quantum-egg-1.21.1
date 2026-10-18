package com.happysadduck.quantumegg.blocks;

import org.jetbrains.annotations.Nullable;

import com.happysadduck.quantumegg.registry.mod_blocks_t;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public class quantum_box_entity_t extends BlockEntity implements Inventory {
    private DefaultedList<ItemStack> items = DefaultedList.ofSize(1, ItemStack.EMPTY);
    private boolean triggered;

    public quantum_box_entity_t(BlockPos pos, BlockState state) {
        super(mod_blocks_t.QUANTUM_BOX_ENTITY, pos, state);
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return slot == 0 && items.get(0).isEmpty();
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return items.get(0).isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return slot == 0 ? items.get(0) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (slot != 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = items.get(0).split(amount);
        markDirty();
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
        }
        return stack;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = items.get(0);
        items.set(0, ItemStack.EMPTY);
        markDirty();
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
        }
        return stack;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot != 0) {
            return;
        }

        ItemStack copy = stack.copy();
        if (!copy.isEmpty()) {
            copy.setCount(1);
        }

        items.set(0, copy);
        markDirty();
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        items.set(0, ItemStack.EMPTY);
        markDirty();
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        Inventories.writeNbt(nbt, items, registries);
        nbt.putBoolean("triggered", triggered);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);

        items = DefaultedList.ofSize(1, ItemStack.EMPTY);
        Inventories.readNbt(nbt, items, registries);
        triggered = nbt.getBoolean("triggered");

        ItemStack stack = items.get(0);
        if (!stack.isEmpty() && stack.getCount() > 1) {
            stack.setCount(1);
        }
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbt = new NbtCompound();
        this.writeNbt(nbt, registries);
        return nbt;
    }

    public Boolean get_triggered() {
        return triggered;
    }

    public void set_triggered(Boolean new_triggered_value) {
        triggered = new_triggered_value;
        markDirty();
    }
}
