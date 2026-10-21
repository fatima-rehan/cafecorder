package com.cafecorder.model;

// What kind of place it is, for filtering. A visit can have several (a Yemeni tea house is both).
// Stored by name, so new kinds can be added anywhere in the list.
public enum CafeKind {
    COFFEE,
    MATCHA,
    TEA_HOUSE,
    YEMENI
}
