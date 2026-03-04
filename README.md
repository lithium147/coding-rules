
# TODO create syntactic sugar library

# ${project.version} can be excluded from the enforcer rule as it is a special property that is commonly used
# in fact, ${project.*} can be excluded

Split the enforcer into two rules:
- one for checking the existing properties
  - is the property used more than once?
    - if not, it must be removed
  - extract all artifacts from model
    - versionUsages = group by version, ignoring explicit properties
  - for each property:
    - versionUsages.get(property.name).size() -> usages of the property
      - if usages==1, then the property should be inlined and removed
        - please replace usgae of 'property.name' with 'property.value'
      - if usages==0, then the property should be removed
        - please remove 'property.name'
    - what about if there is a version that is the same as the property value but not using the property?
      - This should be replaced with the property as well
      - and same rules apply to this version as well
      - so if usage==1, but would be usage==2 if we replace it with the property, then we should replace it with the property and keep the property
      - this means that versionUsages must capture both explicit and implicit usages of the version
      - perhaps the artifact model should cover both these cases
        - getVersion() returns the resolved version
          - how to get the resolved version?
            - perhaps this can come from the effective model
            - how populate the resolved version?
            - at time of building artifact model or as an enrichment step after building the artifact model?
        - getVersionProperty() returns the property if it exists, otherwise null
      - 
        - if the version is used more than once, it should be replaced with the property
        - if the version is used only once, it should be inlined and removed
- one for checking repeated versions
  - if a version is repeated, it must be replaced with a property

- what about if there is a property but its not being used?
  - or there is a version that is the same as the property value but not using the property?
    - This should be replaced with the property as well


