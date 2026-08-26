with open('app/src/main/java/com/example/domain/repository/BillingRepository.kt', 'r') as f:
    content = f.read()
content = content.replace('val subscriptionProducts: StateFlow<List<ProductDetails>>', 'val subscriptionProducts: StateFlow<List<ProductDetails>?>')
with open('app/src/main/java/com/example/domain/repository/BillingRepository.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/data/repository/BillingRepositoryImpl.kt', 'r') as f:
    content = f.read()
content = content.replace('private val _subscriptionProducts = MutableStateFlow<List<ProductDetails>>(emptyList())', 'private val _subscriptionProducts = MutableStateFlow<List<ProductDetails>?>(null)')
content = content.replace('override val subscriptionProducts: StateFlow<List<ProductDetails>> = _subscriptionProducts', 'override val subscriptionProducts: StateFlow<List<ProductDetails>?> = _subscriptionProducts')
with open('app/src/main/java/com/example/data/repository/BillingRepositoryImpl.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/premium/PremiumViewModel.kt', 'r') as f:
    content = f.read()
content = content.replace('val subscriptionProducts: StateFlow<List<ProductDetails>> = billingRepository.subscriptionProducts\n        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())', 'val subscriptionProducts: StateFlow<List<ProductDetails>?> = billingRepository.subscriptionProducts\n        .stateIn(viewModelScope, SharingStarted.Lazily, null)')
with open('app/src/main/java/com/example/ui/premium/PremiumViewModel.kt', 'w') as f:
    f.write(content)
