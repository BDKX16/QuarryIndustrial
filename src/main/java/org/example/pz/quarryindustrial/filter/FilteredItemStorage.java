package org.example.pz.quarryindustrial.filter;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.util.Iterator;

/**
 * Un inventario visto a través del filtro de la cara del caño: lo que el filtro no deja pasar ni se puede meter ni se
 * puede sacar (y los casilleros con ese ítem se ven vacíos). Ala mueve los ítems con {@code StorageUtil.move} entre
 * dos de estos, así que alcanza con filtrar acá.
 */
public final class FilteredItemStorage implements Storage<ItemVariant> {
    private final Storage<ItemVariant> delegate;
    private final PipeFilterData filter;

    public FilteredItemStorage(Storage<ItemVariant> delegate, PipeFilterData filter) {
        this.delegate = delegate;
        this.filter = filter;
    }

    @Override
    public boolean supportsInsertion() {
        return delegate.supportsInsertion();
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        return filter.passes(resource) ? delegate.insert(resource, maxAmount, transaction) : 0;
    }

    @Override
    public boolean supportsExtraction() {
        return delegate.supportsExtraction();
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        return filter.passes(resource) ? delegate.extract(resource, maxAmount, transaction) : 0;
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        Iterator<StorageView<ItemVariant>> inner = delegate.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return inner.hasNext();
            }

            @Override
            public StorageView<ItemVariant> next() {
                return new FilteredView(inner.next());
            }
        };
    }

    @Override
    public long getVersion() {
        return delegate.getVersion();
    }

    private final class FilteredView implements StorageView<ItemVariant> {
        private final StorageView<ItemVariant> view;

        private FilteredView(StorageView<ItemVariant> view) {
            this.view = view;
        }

        private boolean visible() {
            return !view.isResourceBlank() && filter.passes(view.getResource());
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            return filter.passes(resource) ? view.extract(resource, maxAmount, transaction) : 0;
        }

        @Override
        public boolean isResourceBlank() {
            return !visible();
        }

        @Override
        public ItemVariant getResource() {
            return visible() ? view.getResource() : ItemVariant.blank();
        }

        @Override
        public long getAmount() {
            return visible() ? view.getAmount() : 0;
        }

        @Override
        public long getCapacity() {
            return view.getCapacity();
        }

        @Override
        public StorageView<ItemVariant> getUnderlyingView() {
            return view.getUnderlyingView();
        }
    }
}
