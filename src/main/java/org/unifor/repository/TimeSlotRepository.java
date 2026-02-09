package org.unifor.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.unifor.entity.TimeSlot;

@ApplicationScoped
public class TimeSlotRepository implements PanacheRepository<TimeSlot> {
}
