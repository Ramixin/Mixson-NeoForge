package net.ramixin.mixson.events;

public interface MixsonEventTypes {

    interface BaseEvent { }

    interface Creation extends BaseEvent { }

    interface Deletion extends BaseEvent { }

    interface Modification extends BaseEvent { }


}
