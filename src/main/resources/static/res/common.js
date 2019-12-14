function msgBox(title, content) {
    $('.basic.modal .header').html(title);
    $('.basic.modal .description').html('<p>' + content + '</p>');
    $('.basic.modal').modal('show')
}

let rule = {type: '选择广告类型', fee: '选择计费方式', deal: '选择排期类型', name: '填写广告名', package: '选择版位', begin: '选择起始时间', end: '选择结束时间', amount: '填写广告数量'};

function initRules() {
    let dict = {};
    for (let k in rule) {
        dict[k] = {identifier: k, rules: [{type: 'empty', prompt: rule[k]}]}
    }
    rule = dict;
}

function initEvents() {
    let date = new Date();
    $('.ui.menu .item').tab();
    $('.ui.dropdown').dropdown();
    $('.ui.menu .item.home').click();
    $('input[name="name"]').val('压测' + date.getFullYear() + (date.getMonth() < 9 ? '0' : '') + (date.getMonth() + 1) + (date.getDate() < 10 ? '0' : '') + date.getDate());
    $('input[name="begin"]').val(date.getFullYear() + '-' + (date.getMonth() < 9 ? '0' : '') + (date.getMonth() + 1) + '-' + (date.getDate() < 10 ? '0' : '') + date.getDate());
    $('input[name="type"]').bind('change', function () {
        let deal = $('#deal');
        console.info(0);
        if ($(this).val() === '1') {
            $('input[name="fee"]').siblings('.menu').html('<div class="item" data-value="1">CPT</div><div class="item" data-value="2">CPM</div>');
            deal.removeClass('hidden');
            deal.removeClass('required');
            //rule['deal']['optional'] = true;
        }
        else {
            $('input[name="fee"]').siblings('.menu').html('<div class="item" data-value="3">CPC</div><div class="item" data-value="2">CPM</div>');
            deal.addClass('hidden');
            deal.addClass('required');
            //rule['deal']['optional'] = false;
        }
    });
    $('.ui.blue.basic.inverted.button').bind('click', function () {
        $('.basic.modal').modal('hide')
    });
    $('#create').bind('click', function () {
        $('.ui.form').form(rule, {inline: true, on: 'blur'});
        console.info(1)
    })
}

function initData() {
    $.ajax({
        url: '/j/list',
        type: 'get',
        success: function (data) {
            let html = '';
            for(let v of data) {
                html += '<option value="' + v.id + '">' + v.name + '</option>';
            }
            $('select[name="package"]').html(html);
            $('.ui.form').removeClass('loading')
        },
        error: function(XMLHttpRequest) {
            if (XMLHttpRequest) msgBox(XMLHttpRequest.status, XMLHttpRequest.statusText)
        }
    })
}

$(document).ready(() => {
    initRules();
    initEvents();
    //initData()
    $('.ui.form').removeClass('loading')
});



