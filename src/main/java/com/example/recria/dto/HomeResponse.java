package com.example.recria.dto;

import java.util.List;

public class HomeResponse {
    private List<EventResponse> upcomingEvents;
    private List<EventResponse> notifications;
    private List<EventResponse> unreflectedEvents;

    public List<EventResponse> getUpcomingEvents() { return upcomingEvents; }
    public void setUpcomingEvents(List<EventResponse> upcomingEvents) { this.upcomingEvents = upcomingEvents; }
    public List<EventResponse> getNotifications() { return notifications; }
    public void setNotifications(List<EventResponse> notifications) { this.notifications = notifications; }
    public List<EventResponse> getUnreflectedEvents() { return unreflectedEvents; }
    public void setUnreflectedEvents(List<EventResponse> unreflectedEvents) { this.unreflectedEvents = unreflectedEvents; }
}