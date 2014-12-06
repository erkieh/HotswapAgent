Wicket plugin
=============
 Reinjects Spring beans into @SpringBean annotated fields in Pages retrieved from the AbstractPageManager. Component
 fields in these pages will also be reinjected. Other classes like IDataProvider-s which might be doing manual
 injection in their constructors, will not be processed.