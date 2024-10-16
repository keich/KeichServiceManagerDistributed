package ru.keich.mon.servicemanager.item;

import lombok.Getter;
import ru.keich.mon.servicemanager.BaseStatus;

@Getter
public class ItemToEvent implements Comparable<ItemToEvent> {
	
	private final String eventId;
	private final BaseStatus status;

	public ItemToEvent(String eventId, BaseStatus status) {
		super();
		this.eventId = eventId;
		this.status = status;
	}

	@Override
	public int hashCode() {
		return eventId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ItemToEvent other = (ItemToEvent) obj;
		return eventId.equals(other.eventId);
	}

	@Override
	public int compareTo(ItemToEvent o) {
		var ret = eventId.compareTo(o.eventId);
		if (ret == 0) {
			return status.compareTo(o.status);
		}
		return ret;
	}

}
