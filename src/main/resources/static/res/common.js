function getNow() {
    let date = new Date();
    return date.getFullYear() + (date.getMonth() < 9 ? '0' : '') + (date.getMonth() + 1) + (date.getDate() < 10 ? '0' : '') + date.getDate()
}

function getDate() {
    let date = new Date();
    return date.getFullYear() + '-' + (date.getMonth() < 9 ? '0' : '') + (date.getMonth() + 1) + '-' + (date.getDate() < 10 ? '0' : '') + date.getDate()
}

$(document).ready(() => {
    $('.ui.menu .item').tab();
    $('.ui.dropdown').dropdown();
    $('.ui.menu .item.home').click();
    $('input[name="name"]').val('压测' + getNow());
    $('input[name="begin"]').val(getDate());
});

$('input[name="type"]').bind('change', function () {
    if ($(this).val() === '1') {
        $('input[name="fee"]').siblings('.menu').html('<div class="item" data-value="1">CPT</div><div class="item" data-value="2">CPM</div>');
        $('#deal').removeClass('hidden');
    }
    else {
        $('input[name="fee"]').siblings('.menu').html('<div class="item" data-value="3">CPC</div><div class="item" data-value="2">CPM</div>');
        $('#deal').addClass('hidden');
    }
});

