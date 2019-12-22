function msgBox(title, content) {
    $('.modal .header').html(title);
    $('.modal .description').html('<p>' + content + '</p>');
    $('.modal').modal('show')
}

function parseDate(date, connector) {
    let s = connector ? connector : '';
    return date.getFullYear() + s + (date.getMonth() < 9 ? '0' : '') + (date.getMonth() + 1) + s + (date.getDate() < 10 ? '0' : '') + date.getDate()
}

function compareDate(date1, date2) {
    if (date1.constructor === String) date1 = new Date(date1);
    if (date2.constructor === String) date2 = new Date(date2);
    if (date1.getFullYear() !== date2.getFullYear()) return date1.getFullYear() > date2.getFullYear() ? 1 : -1;
    if (date1.getMonth() !== date2.getMonth()) return date1.getMonth() > date2.getMonth() ? 1 : -1;
    if (date1.getDate() !== date2.getDate()) return date1.getDate() > date2.getDate() ? 1 : -1;
    return 0
}

function divide(value, second) {
    if (!value) return '';
    let i = value.indexOf('_');
    if (i === -1) return '';
    return second ? value.substring(i + 1) : value.substring(0, i);
}

function format(str, val) {
    if (val.constructor === Array) {
        let i = 0;
        for (let v of val) {
            i = str.indexOf('{}', i);
            if (i === -1) break;
            str = str.substring(0, i) + v + str.substring(i + 2);
            i += v.length;
        }
        return str;
    }
    let i = str.indexOf('{}');
    return str.substring(0, i) + val + str.substring(i + 2);
}

function fillOptions(name, options) {
    let html = '';
    let idx = 1;
    for (let opt of options) {
        html += format('<option class="item" data-value="{}">{}</option>',
            opt.constructor === String ? [idx++, opt] : [opt.value, opt.name]);
    }
    let p = $(format('input[name="{}"]', name));
    p.nextAll('.menu').html(html);
    if (!options || options.length < 1) return;
    p.nextAll('.text').html(options[0].constructor === String ? options[0] : options[0].name);
    p.val(options[0].constructor === String ? '1' : options[0].value.toString())
}

function setDisabled(name, flag) {
    let p = $(format('input[name="{}"]', name)).parent();
    if (flag) {
        p.addClass('disabled');
        p.prev().attr('style', 'color: rgba(34, 36, 38, 0.15)')
    }
    else {
        p.removeClass('disabled');
        p.prev().removeAttr('style')
    }
}

function composeData() {
    let data = {};
    $('input').each(function () {
        let name = $(this).attr('name');
        if (name) data[name] = $(this).val()
    });
    data.flight = divide(data.flight);
    if (!data.flight) { msgBox('提示', '请选择广告位'); return false }
    if (!data.type) { msgBox('提示', '请选择广告类型'); return false }
    let amount = parseInt(data.amount);
    if (isNaN(amount) || amount < 1) { msgBox('提示', '请输入正确的广告数量'); return false }

    if (!data.deal) data.deal = 1;
    if (!data.fee) data.fee = parseInt(data.type) === 1 && parseInt(data.deal) === 2 ? 2 : 1;
    if (!data.flow) data.flow = 1;
    let date = new Date();
    if (!data.name) data.name = '压测' + parseDate(date);
    if (!data.begin) data.begin = parseDate(date, '-');
    if (!data.end) {
        date = new Date(date.begin);
        date.setDate(date.getDate() + 7);
        data.end = parseDate(date, '-');
    }
    if (compareDate(data.begin, data.end) >= 0) { msgBox('提示', '结束时间必须在开始时间之后'); return false }
    return data
}

function initEvents() {
    $.fn.api.settings.api = {
        'create': '/j/create',
        'query' : '/j/flight/{query}'
    };
    $('.ui.selection.dropdown').dropdown();
    $('input[name="flight"]').bind('change', function () {
        let v = divide($(this).val(), true);
        let opt = [];
        if (v === '1' || v === '3') opt.push({name: '合约', value: 1});
        if (v === '2' || v === '3') opt.push({name: '竞价', value: 2});
        fillOptions('type', opt)
    });
    $('input[name="type"]').bind('change', function () {
        if ($(this).val() === '1') {
            setDisabled('deal', false);
            setDisabled('flow', false);
            fillOptions('fee', ['CPT', 'CPM'])
        }
        else {
            setDisabled('deal', true);
            setDisabled('flow', true);
            fillOptions('fee', ['CPC', 'CPM'])
        }
    });
    $('input[name="deal"]').bind('change', function () {
        let v = $(this).val();
        if (v === '1') {
            setDisabled('flow', false);
            fillOptions('fee', ['CPT', 'CPM'])
        }
        else if (v === '2') {
            setDisabled('flow', true);
            fillOptions('fee', [{name: 'CPM', value: 2}])
        }
        else  {
            setDisabled('flow', false);
            fillOptions('fee', ['CPT'])
        }
    });
    $('input[name="fee"]').bind('change', function () {
        setDisabled('flow', !($(this).val() === '1' && $('input[name="type"]').val() === '1'));
    });
    $('#settings').bind('click', function () {
        $('#more').transition('slide down')
    });
    $('.ui.search.dropdown').dropdown({
        apiSettings: {
            action: 'query',
            beforeSend: function (settings) {
                if (settings.urlData.query.length < 2) return false;
                return settings
            }
        }
    });
    $('#create').api({
        action: 'create',
        method: 'post',
        beforeSend: function (settings) {
            let data = composeData();
            if (!data) return false;
            settings.data = data;
            console.info(settings);
            $('#create').addClass('loading');
            return settings;
        },
        onResponse: function (response) {
            $('#create').removeClass('loading');
            if (response) {
                if (response.success) msgBox('创建成功', '广告单创建成功！');
                else msgBox('创建失败', response.message);
            } else msgBox('创建失败', '无响应！');
            return response;
        },
        onError: function (errorMessage) {
            msgBox('创建失败', errorMessage);
        }
    });

}

function initData() {
    let date = new Date();
    $('input[name="name"]').val('压测' + parseDate(date));
    $('input[name="begin"]').val(parseDate(date, '-'));
    date.setDate(date.getDate() + 7);
    $('input[name="end"]').val(parseDate(date, '-'));
}

$(document).ready(() => {
    initEvents();
    initData()
});



