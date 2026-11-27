package com.petCare.petCare.domain.realtime.service;

import com.petCare.petCare.domain.realtime.dto.FamilyRealtimeEvent;

public interface FamilyEventBroker {

	void publish(FamilyRealtimeEvent event);

}
